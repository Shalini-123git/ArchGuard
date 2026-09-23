import { useEffect, useMemo, useRef } from 'react';
import cytoscape from 'cytoscape';

const LAYER_COLORS = ['#2563eb', '#0f766e', '#7c3aed', '#c2410c', '#4d7c0f', '#be185d'];

function layerColor(layer) {
  let value = 0;
  for (const character of layer) value = ((value << 5) - value) + character.charCodeAt(0);
  return LAYER_COLORS[Math.abs(value) % LAYER_COLORS.length];
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

/** Displays the persisted dependency graph using Cytoscape's built-in CoSE layout. */
export default function DependencyGraph({ graph, visibleNodes, selectedViolation, selectedNode, onNodeSelect }) {
  const container = useRef(null);
  const visibleIds = useMemo(() => new Set(visibleNodes.map((node) => node.id)), [visibleNodes]);

  useEffect(() => {
    if (!container.current) return undefined;
    const visibleEdges = graph.dependencies.filter((edge) => visibleIds.has(edge.from) && visibleIds.has(edge.to));
    const radius = selectedNode ? blastRadius(selectedNode, visibleEdges) : new Set();
    const elements = [
      ...visibleNodes.map((node) => ({ data: { id: node.id, label: node.id, color: layerColor(node.layer) }, classes: [node.cycleMember && 'cycle', radius.has(node.id) && 'blast'].filter(Boolean).join(' ') })),
      ...visibleEdges.map((edge) => ({ data: { id: edge.id, source: edge.from, target: edge.to }, classes: [edge.violationIds.length && 'violation', selectedViolation && edge.violationIds.includes(selectedViolation.id) && 'focused'].filter(Boolean).join(' ') }))
    ];
    const cy = cytoscape({
      container: container.current,
      elements,
      layout: { name: 'cose', animate: false, padding: 36 },
      style: [
        { selector: 'node', style: { 'background-color': 'data(color)', label: 'data(label)', color: '#172033', 'font-size': 10, 'text-wrap': 'wrap', 'text-max-width': 130, 'text-valign': 'bottom', 'text-margin-y': 5, width: 28, height: 28 } },
        { selector: 'edge', style: { width: 2, 'line-color': '#94a3b8', 'target-arrow-color': '#94a3b8', 'target-arrow-shape': 'triangle', 'curve-style': 'bezier' } },
        { selector: 'node.cycle', style: { 'border-width': 4, 'border-color': '#f59e0b', 'border-style': 'double' } },
        { selector: 'edge.violation', style: { 'line-color': '#dc2626', 'target-arrow-color': '#dc2626', width: 3 } },
        { selector: 'node.blast', style: { 'border-width': 5, 'border-color': '#172033' } },
        { selector: 'edge.focused', style: { width: 6, 'line-color': '#831843', 'target-arrow-color': '#831843' } }
      ]
    });
    cy.on('tap', 'node', (event) => onNodeSelect(event.target.id()));
    cy.on('tap', (event) => { if (event.target === cy) onNodeSelect(null); });
    if (selectedViolation) {
      const focused = cy.edges('.focused');
      if (focused.length) cy.fit(focused.union(focused.connectedNodes()), 50);
    }
    return () => cy.destroy();
  }, [graph, visibleNodes, visibleIds, selectedViolation, selectedNode, onNodeSelect]);

  if (!visibleNodes.length) return <div className="graph-empty">No packages match the current filters.</div>;
  return <div className="graph-section"><div className="graph-legend"><span><i className="legend-node" /> Layer</span><span><i className="legend-cycle" /> Cycle member</span><span><i className="legend-edge" /> Violation</span></div><div className="graph-canvas" ref={container} aria-label="Package dependency graph" /></div>;
}
