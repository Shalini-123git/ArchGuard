import { describe, expect, it } from 'vitest';
import { findCycles, getCycleMembers, getViolationEdges, isSimpleLoop, orderedLoop, uniqueLabels } from './graphUtils.js';

const modules = (...ids) => ids.map((id) => ({ id }));
const edges = (...pairs) => pairs.map(([from, to]) => ({ id: `${from}->${to}`, from, to, violationIds: [] }));

describe('graph utilities', () => {
  it('uses different suffixes for packages with the same two-segment ending', () => {
    const labels = uniqueLabels([
      'com.acme.settings.preferences',
      'org.example.settings.preferences'
    ]);

    expect(labels).toEqual(new Map([
      ['com.acme.settings.preferences', 'acme.settings.preferences'],
      ['org.example.settings.preferences', 'example.settings.preferences']
    ]));
  });

  it('uses the shortest unique suffix for an otherwise unique package', () => {
    expect(uniqueLabels(['com.example.web', 'com.example.data']).get('com.example.web')).toBe('web');
  });

  it('returns no cycles for an acyclic graph', () => {
    expect(findCycles(modules('a', 'b', 'c'), edges(['a', 'b'], ['b', 'c']))).toEqual([]);
  });

  it('finds a two-node cycle', () => {
    expect(findCycles(modules('b', 'a'), edges(['a', 'b'], ['b', 'a']))).toEqual([['a', 'b']]);
  });

  it('finds a three-node cycle', () => {
    expect(findCycles(modules('c', 'a', 'b'), edges(['a', 'b'], ['b', 'c'], ['c', 'a']))).toEqual([['a', 'b', 'c']]);
  });

  it('finds and sorts two separate cycles', () => {
    expect(findCycles(
      modules('d', 'c', 'b', 'a'),
      edges(['c', 'd'], ['d', 'c'], ['a', 'b'], ['b', 'a'])
    )).toEqual([['a', 'b'], ['c', 'd']]);
  });

  it('returns violation edges and sorted unique cycle members', () => {
    const dependencies = [
      { id: 'edge-1', from: 'pkg.b', to: 'pkg.a', violationIds: ['violation-1'] },
      { id: 'edge-2', from: 'pkg.a', to: 'pkg.c', violationIds: ['violation-1'] },
      { id: 'edge-3', from: 'pkg.c', to: 'pkg.b', violationIds: ['other'] }
    ];
    const violation = { id: 'violation-1' };
    expect(getViolationEdges(violation, dependencies)).toEqual(dependencies.slice(0, 2));
    expect(getCycleMembers(violation, dependencies)).toEqual(['pkg.a', 'pkg.b', 'pkg.c']);
  });

  it('recognizes and orders one simple loop', () => {
    const loopEdges = edges(['pkg.a', 'pkg.b'], ['pkg.b', 'pkg.c'], ['pkg.c', 'pkg.a']);

    expect(isSimpleLoop(['pkg.a', 'pkg.b', 'pkg.c'], loopEdges)).toBe(true);
    expect(orderedLoop(['pkg.a', 'pkg.b', 'pkg.c'], loopEdges)).toEqual(['pkg.a', 'pkg.b', 'pkg.c', 'pkg.a']);
  });

  it('rejects a dense cluster as a simple loop', () => {
    const clusterEdges = edges(
      ['pkg.a', 'pkg.b'], ['pkg.a', 'pkg.c'], ['pkg.b', 'pkg.c'], ['pkg.b', 'pkg.d'],
      ['pkg.c', 'pkg.d'], ['pkg.c', 'pkg.e'], ['pkg.d', 'pkg.e'], ['pkg.d', 'pkg.a'],
      ['pkg.e', 'pkg.a'], ['pkg.e', 'pkg.b']
    );

    expect(isSimpleLoop(['pkg.a', 'pkg.b', 'pkg.c', 'pkg.d', 'pkg.e'], clusterEdges)).toBe(false);
    expect(orderedLoop(['pkg.a', 'pkg.b', 'pkg.c', 'pkg.d', 'pkg.e'], clusterEdges)).toBeNull();
  });
});
