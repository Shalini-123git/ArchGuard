import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ViolationList from './ViolationList.jsx';

const dependency = (id, from, to, violationIds) => ({ id, from, to, violationIds });

describe('ViolationList', () => {
  it('renders a forbidden dependency card and selects it from both controls', () => {
    const onSelect = vi.fn();
    const violation = {
      id: 'v1', ruleId: 'forbidden:controller->repository', severity: 'high',
      fromModule: 'com.example.controller', toModule: 'com.example.repository', blastRadiusCount: 1
    };
    render(<ViolationList violations={[violation]} dependencies={[dependency('e1', violation.fromModule, violation.toModule, ['v1'])]} onSelect={onSelect} />);

    expect(screen.getByText('HIGH')).toBeTruthy();
    expect(screen.getByText('controller → repository')).toBeTruthy();
    expect(screen.getByText('Rule: forbidden:controller->repository')).toBeTruthy();
    expect(screen.getByText('Dependency: com.example.controller → com.example.repository')).toBeTruthy();
    expect(screen.getByText('Blast radius: 1 package')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: /View on graph/i }));
    expect(onSelect).toHaveBeenCalledWith(violation);
  });

  it('derives cycle members from violation edges and renders a simple loop path', () => {
    const violation = { id: 'cycle-1', ruleId: 'no-cycles', severity: 'MEDIUM', blastRadiusCount: 2 };
    const dependencies = [
      dependency('e1', 'pkg.a', 'pkg.b', ['cycle-1']),
      dependency('e2', 'pkg.b', 'pkg.c', ['cycle-1']),
      dependency('e3', 'pkg.c', 'pkg.a', ['cycle-1'])
    ];
    render(<ViolationList violations={[violation]} dependencies={dependencies} onSelect={vi.fn()} />);

    expect(screen.getByText('Circular dependency')).toBeTruthy();
    expect(screen.getByText('Packages in cycle: 3')).toBeTruthy();
    expect(screen.getByText('pkg.a → pkg.b → pkg.c → pkg.a')).toBeTruthy();
  });

  it('shows a plain cycle member list when the edges are not one simple loop', () => {
    const violation = { id: 'cycle-2', ruleId: 'no-cycles', severity: 'low', blastRadiusCount: 0 };
    const dependencies = [
      dependency('e1', 'pkg.a', 'pkg.b', ['cycle-2']),
      dependency('e2', 'pkg.a', 'pkg.c', ['cycle-2']),
      dependency('e3', 'pkg.b', 'pkg.a', ['cycle-2']),
      dependency('e4', 'pkg.c', 'pkg.a', ['cycle-2'])
    ];
    render(<ViolationList violations={[violation]} dependencies={dependencies} onSelect={vi.fn()} />);

    expect(screen.getByText('pkg.a, pkg.b, pkg.c')).toBeTruthy();
    expect(screen.queryByText(/pkg\.a →/)).toBeNull();
  });

  it('truncates long cycle cards after four package names', () => {
    const violation = { id: 'cycle-3', ruleId: 'no-cycles', severity: 'medium', blastRadiusCount: 0 };
    const dependencies = [
      dependency('e1', 'pkg.a', 'pkg.b', ['cycle-3']),
      dependency('e2', 'pkg.a', 'pkg.c', ['cycle-3']),
      dependency('e3', 'pkg.b', 'pkg.c', ['cycle-3']),
      dependency('e4', 'pkg.b', 'pkg.d', ['cycle-3']),
      dependency('e5', 'pkg.c', 'pkg.d', ['cycle-3']),
      dependency('e6', 'pkg.c', 'pkg.e', ['cycle-3']),
      dependency('e7', 'pkg.d', 'pkg.e', ['cycle-3']),
      dependency('e8', 'pkg.d', 'pkg.a', ['cycle-3']),
      dependency('e9', 'pkg.e', 'pkg.a', ['cycle-3']),
      dependency('e10', 'pkg.e', 'pkg.b', ['cycle-3'])
    ];
    render(<ViolationList violations={[violation]} dependencies={dependencies} onSelect={vi.fn()} />);

    expect(screen.getByText('Packages in cycle: 5')).toBeTruthy();
    expect(screen.getByText('pkg.a, pkg.b, pkg.c, pkg.d +1 more')).toBeTruthy();
    expect(screen.queryByText(/pkg\.a →/)).toBeNull();
  });

  it('warns when the graph has cycles without a violation', () => {
    const modules = [{ id: 'pkg.a' }, { id: 'pkg.b' }];
    const dependencies = [dependency('e1', 'pkg.a', 'pkg.b', []), dependency('e2', 'pkg.b', 'pkg.a', [])];
    render(<ViolationList violations={[]} modules={modules} dependencies={dependencies} onSelect={vi.fn()} />);

    expect(screen.getByText('⚠ Cycles exist in the graph but no violation was reported.')).toBeTruthy();
    expect(screen.queryByText('✓ No architecture violations detected')).toBeNull();
  });

  it('renders the empty state when the graph has no cycles', () => {
    render(<ViolationList violations={[]} modules={[{ id: 'pkg.a' }, { id: 'pkg.b' }]} dependencies={[]} onSelect={vi.fn()} />);

    expect(screen.getByText('✓ No architecture violations detected')).toBeTruthy();
    expect(screen.getByText('Cycle detection ran on all analyzed packages. Layer rules are checked only if you supplied them.')).toBeTruthy();
  });

  it('activates the card with Enter and Space', () => {
    const onSelect = vi.fn();
    const violation = { id: 'v2', ruleId: 'no-cycles', severity: 'other', blastRadiusCount: 3 };
    render(<ViolationList violations={[violation]} dependencies={[]} onSelect={onSelect} />);
    const card = screen.getByRole('button', { name: /Circular dependency/ });

    fireEvent.keyDown(card, { key: 'Enter' });
    fireEvent.keyDown(card, { key: ' ' });
    expect(onSelect).toHaveBeenCalledTimes(2);
  });
});