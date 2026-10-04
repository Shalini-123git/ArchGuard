import { useEffect, useMemo, useRef } from 'react';
import cytoscape from 'cytoscape';
import { uniqueLabels } from '../graphUtils.js';

const LAYER_COLORS = ['#2563eb', '#0f766e', '#7c3aed', '#c2410c', '#4d7c0f', '#be185d'];
const MIN_ZOOM = 0.25;
const MAX_ZOOM = 3;
const ZOOM_STEP = 1.5;

function layerColor(layer) {
  let value = 0;
  for (const character of layer) value = ((value << 5) - value) + character.charCodeAt(0);
  return LAYER_COLORS[Math.abs(value) % LAYER_COLORS.length];
}

function normalizedLayer(layer) {
  return layer && layer !== 'unknown' ? layer : 'unknown';
}

function blastRadius(nodeId, edges) {
  const incoming = new Map();
  for (const edge of edges) {
    const sources = incoming.get(edge.to) || [];
    sources.push(edge.from);
    incoming.set(edge.to, sources);
  }
  const affected = new Set([nodeId]);
  const pending = [nodeId];
  while (pending.length) {
    for (const source of incoming.get(pending.pop()) || []) {
      if (!affected.has(source)) { affected.add(source); pending.push(source); }
    }
  }
  return affected;
}

/** Displays the persisted dependency graph using Cytoscape's built-in layout. */
export default function DependencyGraph({ graph, visibleNodes, selectedViolation, selectedNode, onNodeSelect, onBackgroundSelect }) {
  const container = useRef(null);
  const cyRef = useRef(null);
  const visibleIds = useMemo(() => new Set(visibleNodes.map((node) => node.id)), [visibleNodes]);
  const visibleEdges = useMemo(() => graph.dependencies.filter((edge) => visibleIds.has(edge.from) && visibleIds.has(edge.to)), [graph.dependencies, visibleIds]);
  const labels = useMemo(() => uniqueLabels(visibleNodes.map((node) => node.id)), [visibleNodes]);
  const legendLayers = useMemo(() => [...new Set(graph.modules.map((node) => normalizedLayer(node.layer)))].map((layer) => ({
    label: layer === 'unknown' ? 'Unassigned' : layer,
    color: layerColor(layer)
  })), [graph.modules]);

  useEffect(() => {
    if (!container.current) return undefined;
    const edgeCurve = visibleEdges.length > 150 ? 'haystack' : 'bezier';
    const elements = [
      ...visibleNodes.map((node) => ({ data: { id: node.id, label: labels.get(node.id), fullLabel: node.id, color: layerColor(normalizedLayer(node.layer)) }, classes: node.cycleMember ? 'cycle' : '' })),
      ...visibleEdges.map((edge) => ({ data: { id: edge.id, source: edge.from, target: edge.to }, classes: edge.violationIds.length ? 'violation' : '' }))
    ];
    const cy = cytoscape({
      container: container.current,
      elements,
      minZoom: MIN_ZOOM,
      maxZoom: MAX_ZOOM,
      style: [
        { selector: 'node', style: { 'background-color': 'data(color)', label: 'data(label)', color: '#172033', 'font-size': 12, 'font-weight': 650, 'text-background-color': '#ffffff', 'text-background-opacity': 1, 'text-background-padding': 3, 'text-wrap': 'wrap', 'text-max-width': 150, 'text-valign': 'bottom', 'text-margin-y': 6, width: 32, height: 32 } },
        { selector: 'node.selected, node.hovered', style: { label: 'data(fullLabel)' } },
        { selector: 'edge', style: { width: 2, opacity: 1, 'line-color': '#94a3b8', 'target-arrow-color': '#94a3b8', 'target-arrow-shape': 'triangle', 'curve-style': edgeCurve } },
        { selector: 'node.cycle', style: { 'border-width': 4, 'border-color': '#f59e0b', 'border-style': 'double' } },
        { selector: 'edge.violation', style: { 'line-color': '#dc2626', 'target-arrow-color': '#dc2626', width: 3 } },
        { selector: 'node.blast', style: { 'border-width': 5, 'border-color': '#172033' } },
        { selector: 'node.dimmed', style: { opacity: .22 } },
        { selector: 'edge.dimmed', style: { opacity: .16 } },
        { selector: 'edge.focused', style: { width: 6, opacity: 1, 'line-color': '#831843', 'target-arrow-color': '#831843' } }
      ]
    });
    cyRef.current = cy;
    cy.layout({ name: 'breadthfirst', directed: true, animate: false, padding: 36, spacingFactor: 1.4 }).run();
    cy.on('tap', 'node', (event) => onNodeSelect(event.target.id()));
    cy.on('mouseover', 'node', (event) => event.target.addClass('hovered'));
    cy.on('mouseout', 'node', (event) => event.target.removeClass('hovered'));
    cy.on('dblclick', (event) => {
      const zoomDirection = event.originalEvent?.shiftKey ? 1 / ZOOM_STEP : ZOOM_STEP;
      const level = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, cy.zoom() * zoomDirection));
      cy.zoom({ level, position: event.position });
    });
    cy.on('zoom', () => {
      if (typeof cy.style !== 'function') return;
      cy.style().selector('node').style('font-size', cy.zoom() < 1.1 ? 0 : 12).update();
    });
    cy.on('tap', (event) => {
      if (event.target === cy) {
        onNodeSelect(null);
        onBackgroundSelect();
      }
    });
    return () => {
      cy.destroy();
      cyRef.current = null;
    };
  }, [labels, onBackgroundSelect, onNodeSelect, visibleEdges, visibleIds, visibleNodes]);

  useEffect(() => {
    const cy = cyRef.current;
    if (!cy) return;
    cy.nodes().forEach((node) => node.removeClass('blast'));
    cy.nodes().forEach((node) => node.removeClass('dimmed selected'));
    cy.edges().forEach((edge) => edge.removeClass('focused dimmed'));

    if (selectedNode) {
      const radius = blastRadius(selectedNode, visibleEdges);
      cy.nodes().forEach((node) => {
        if (radius.has(node.id())) node.addClass('blast');
      });
    }
    if (selectedViolation) {
      const relatedNodes = new Set();
      cy.edges().forEach((edge) => {
        const edgeData = visibleEdges.find((visibleEdge) => visibleEdge.id === edge.id());
        if (edgeData?.violationIds.includes(selectedViolation.id)) {
          edge.addClass('focused');
          relatedNodes.add(edgeData.from);
          relatedNodes.add(edgeData.to);
        } else edge.addClass('dimmed');
      });
      cy.nodes().forEach((node) => {
        if (!relatedNodes.has(node.id())) node.addClass('dimmed');
      });
      if (selectedNode) cy.nodes().forEach((node) => {
        if (node.id() === selectedNode) node.addClass('selected');
      });
      const focused = cy.edges('.focused');
      if (focused.length) cy.fit(focused.union(focused.connectedNodes()), 50);
      else cy.fit(cy.elements(), 50);
    } else {
      if (selectedNode) cy.nodes().forEach((node) => {
        if (node.id() === selectedNode) node.addClass('selected');
      });
      cy.fit(cy.elements(), 50);
    }
  }, [selectedNode, selectedViolation, visibleEdges]);

  useEffect(() => {
    const element = container.current;
    const cy = cyRef.current;
    if (!element || !cy || typeof ResizeObserver === 'undefined') return undefined;
    let timer;
    const observer = new ResizeObserver(() => {
      window.clearTimeout(timer);
      timer = window.setTimeout(() => {
        cy.resize();
        cy.fit(cy.elements(), 50);
      }, 150);
    });
    observer.observe(element);
    return () => {
      window.clearTimeout(timer);
      observer.disconnect();
    };
  }, [visibleNodes.length]);

  function fitGraph() {
    const cy = cyRef.current;
    if (cy) {
      cy.resize();
      cy.fit(cy.elements(), 50);
    }
  }

  if (!visibleNodes.length) return <div className="graph-empty">No packages match the current filters.</div>;
  return <div className="graph-section"><div className="graph-toolbar"><div className="graph-legend">{legendLayers.map(({ label, color }) => <span key={label}><i className="legend-node" style={{ background: color }} /> {label}</span>)}<span><i className="legend-cycle" /> Cycle member</span><span><i className="legend-violation" /> Violation dependency</span><span><i className="legend-normal" /> Normal dependency</span></div><button type="button" className="secondary small-button graph-fit" onClick={fitGraph}>Fit</button></div><div className="graph-canvas" ref={container} aria-label="Package dependency graph. Double-click to zoom in; Shift-double-click to zoom out." /></div>;
}
