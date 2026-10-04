import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import cytoscape from 'cytoscape';
import DependencyGraph from './DependencyGraph.jsx';

vi.mock('cytoscape', () => ({ default: vi.fn() }));

describe('DependencyGraph lifecycle', () => {
  beforeEach(() => vi.clearAllMocks());

  it('uses unique suffix labels while retaining full package names', () => {
    const cy = {
      destroy: vi.fn(),
      edges: vi.fn(() => []),
      elements: vi.fn(() => []),
      fit: vi.fn(),
      layout: vi.fn(() => ({ run: vi.fn() })),
      nodes: vi.fn(() => []),
      on: vi.fn()
    };
    cytoscape.mockReturnValue(cy);
    const modules = [
      { id: 'com.acme.settings.preferences', layer: 'data', cycleMember: false },
      { id: 'org.example.settings.preferences', layer: 'data', cycleMember: false }
    ];

    render(<DependencyGraph graph={{ modules, dependencies: [] }} visibleNodes={modules} selectedViolation={null} selectedNode={null} onNodeSelect={vi.fn()} onBackgroundSelect={vi.fn()} />);

    const nodeElements = cytoscape.mock.calls[0][0].elements.slice(0, 2);
    expect(nodeElements.map(({ data }) => data.label)).toEqual(['acme.settings.preferences', 'example.settings.preferences']);
    expect(nodeElements.map(({ data }) => data.fullLabel)).toEqual(modules.map(({ id }) => id));
  });

  it('limits zoom and supports double-click zoom controls', () => {
    const zoom = vi.fn(() => 1);
    const cy = {
      destroy: vi.fn(),
      edges: vi.fn(() => []),
      elements: vi.fn(() => []),
      fit: vi.fn(),
      layout: vi.fn(() => ({ run: vi.fn() })),
      nodes: vi.fn(() => []),
      on: vi.fn(),
      zoom
    };
    cytoscape.mockReturnValue(cy);

    render(<DependencyGraph graph={{ modules: [{ id: 'pkg.app', layer: 'app', cycleMember: false }], dependencies: [] }} visibleNodes={[{ id: 'pkg.app', layer: 'app', cycleMember: false }]} selectedViolation={null} selectedNode={null} onNodeSelect={vi.fn()} onBackgroundSelect={vi.fn()} />);

    expect(cytoscape.mock.calls[0][0]).toMatchObject({ minZoom: 0.25, maxZoom: 3 });
    const doubleClickHandler = cy.on.mock.calls.find(([eventName]) => eventName === 'dblclick')[1];
    doubleClickHandler({ position: { x: 10, y: 20 }, originalEvent: {} });
    expect(zoom).toHaveBeenLastCalledWith({ level: 1.5, position: { x: 10, y: 20 } });
    doubleClickHandler({ position: { x: 10, y: 20 }, originalEvent: { shiftKey: true } });
    expect(zoom).toHaveBeenLastCalledWith({ level: 0.6666666666666666, position: { x: 10, y: 20 } });
  });

  it('shows one colored legend entry per layer and unassigned modules', () => {
    const cy = {
      destroy: vi.fn(),
      edges: vi.fn(() => []),
      elements: vi.fn(() => []),
      fit: vi.fn(),
      layout: vi.fn(() => ({ run: vi.fn() })),
      nodes: vi.fn(() => []),
      on: vi.fn()
    };
    cytoscape.mockReturnValue(cy);
    const modules = [
      { id: 'pkg.app', layer: 'app', cycleMember: false },
      { id: 'pkg.shared', layer: 'shared', cycleMember: false },
      { id: 'pkg.unknown', layer: 'unknown', cycleMember: false },
      { id: 'pkg.missing', cycleMember: false }
    ];

    render(<DependencyGraph graph={{ modules, dependencies: [] }} visibleNodes={modules} selectedViolation={null} selectedNode={null} onNodeSelect={vi.fn()} onBackgroundSelect={vi.fn()} />);

    expect(screen.getByText('app')).toBeTruthy();
    expect(screen.getByText('shared')).toBeTruthy();
    expect(screen.getByText('Unassigned')).toBeTruthy();
    expect(screen.getAllByText('Unassigned')).toHaveLength(1);
  });

  it('creates and lays out once while selection changes', () => {
    const nodeElements = ['pkg.a', 'pkg.b', 'pkg.c'].map((id) => ({ id: () => id, addClass: vi.fn(), removeClass: vi.fn() }));
    const edgeElements = ['edge-1', 'edge-2'].map((id) => ({ id: () => id, addClass: vi.fn(), removeClass: vi.fn() }));
    const focusedEdges = { length: 1, union: vi.fn(() => ({})), connectedNodes: vi.fn(() => ({})) };
    const layout = { run: vi.fn() };
    const cy = {
      destroy: vi.fn(),
      edges: vi.fn((selector) => selector ? focusedEdges : edgeElements),
      elements: vi.fn(() => []),
      fit: vi.fn(),
      layout: vi.fn(() => layout),
      nodes: vi.fn(() => nodeElements),
      on: vi.fn()
    };
    cytoscape.mockReturnValue(cy);
    const graph = {
      modules: [{ id: 'pkg.a', layer: 'web', cycleMember: false }, { id: 'pkg.b', layer: 'data', cycleMember: false }, { id: 'pkg.c', layer: 'data', cycleMember: false }],
      dependencies: [{ id: 'edge-1', from: 'pkg.a', to: 'pkg.b', violationIds: ['violation-1'] }, { id: 'edge-2', from: 'pkg.b', to: 'pkg.c', violationIds: [] }]
    };
    const violation = { id: 'violation-1' };
    const visibleNodes = graph.modules;
    const onNodeSelect = vi.fn();
    const onBackgroundSelect = vi.fn();
    const view = (selectedViolation = null, selectedNode = null) => render(
      <DependencyGraph graph={graph} visibleNodes={visibleNodes} selectedViolation={selectedViolation} selectedNode={selectedNode} onNodeSelect={onNodeSelect} onBackgroundSelect={onBackgroundSelect} />
    );

    const rendered = view();
    rendered.rerender(<DependencyGraph graph={graph} visibleNodes={visibleNodes} selectedViolation={violation} selectedNode={null} onNodeSelect={onNodeSelect} onBackgroundSelect={onBackgroundSelect} />);
    expect(edgeElements[0].addClass).toHaveBeenCalledWith('focused');
    expect(edgeElements[1].addClass).toHaveBeenCalledWith('dimmed');
    expect(nodeElements[2].addClass).toHaveBeenCalledWith('dimmed');
    rendered.rerender(<DependencyGraph graph={graph} visibleNodes={visibleNodes} selectedViolation={null} selectedNode="pkg.b" onNodeSelect={onNodeSelect} onBackgroundSelect={onBackgroundSelect} />);

    expect(cytoscape).toHaveBeenCalledTimes(1);
    expect(cy.layout).toHaveBeenCalledTimes(1);
    expect(layout.run).toHaveBeenCalledTimes(1);
    expect(edgeElements[0].removeClass).toHaveBeenCalledWith('focused dimmed');
    expect(edgeElements[1].removeClass).toHaveBeenCalledWith('focused dimmed');
    expect(nodeElements[2].removeClass).toHaveBeenCalledWith('dimmed selected');
    expect(cy.fit).toHaveBeenCalled();
    expect(screen.getByText('web')).toBeTruthy();
    expect(screen.getByText('data')).toBeTruthy();
    expect(screen.getByText('Cycle member')).toBeTruthy();
    expect(screen.getByText('Violation dependency')).toBeTruthy();
    expect(screen.getByText('Normal dependency')).toBeTruthy();
  });
});