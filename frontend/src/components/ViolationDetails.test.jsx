import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import ViolationDetails from './ViolationDetails.jsx';

const dependencies = [
  { id: 'edge-1', from: 'pkg.a', to: 'pkg.b', violationIds: ['cycle-1'] },
  { id: 'edge-2', from: 'pkg.b', to: 'pkg.c', violationIds: ['cycle-1'] },
  { id: 'edge-3', from: 'pkg.c', to: 'pkg.a', violationIds: ['cycle-1'] }
];

describe('ViolationDetails', () => {
  it('renders the empty panel', () => {
    render(<ViolationDetails violation={null} dependencies={[]} />);

    expect(screen.getByText('DETAILS')).toBeTruthy();
    expect(screen.getByText('Select a violation to focus its dependency and view its analysis.')).toBeTruthy();
  });

  it('renders a normal violation and its affected packages', () => {
    render(<ViolationDetails dependencies={[]} violation={{
      id: 'v1', severity: 'high', ruleId: 'forbidden:web->repository', fromModule: 'pkg.web', toModule: 'pkg.repo',
      blastRadiusCount: 2, affectedModules: ['pkg.service'], explanation: 'Keep layers separate.', explanationFallback: false
    }} />);

    expect(screen.getByText('VIOLATION DETAILS')).toBeTruthy();
    expect(screen.getByText('HIGH')).toBeTruthy();
    expect(screen.getByText('pkg.web → pkg.repo')).toBeTruthy();
    expect(screen.getByText('2 packages')).toBeTruthy();
    expect(screen.getByText('pkg.service')).toBeTruthy();
    expect(screen.getByText('Groq explanation')).toBeTruthy();
  });

  it('renders the fallback badge', () => {
    render(<ViolationDetails dependencies={[]} violation={{
      id: 'v2', severity: 'medium', ruleId: 'rule', fromModule: 'a', toModule: 'b',
      blastRadiusCount: 1, affectedModules: [], explanation: 'Fallback details.', explanationFallback: true
    }} />);

    expect(screen.getByText('Fallback explanation (AI service was unavailable)')).toBeTruthy();
    expect(screen.queryByText('Groq explanation')).toBeNull();
  });

  it('renders the missing explanation message without a provider label', () => {
    render(<ViolationDetails dependencies={[]} violation={{
      id: 'v3', severity: 'low', ruleId: 'rule', fromModule: 'a', toModule: 'b',
      blastRadiusCount: 0, affectedModules: [], explanation: null, explanationFallback: false
    }} />);

    expect(screen.getByText('No explanation available for this violation.')).toBeTruthy();
    expect(screen.queryByText('Groq explanation')).toBeNull();
    expect(screen.queryByText(/Fallback explanation/)).toBeNull();
  });

  it('shows cycle packages as a list and counts its dependencies', () => {
    render(<ViolationDetails dependencies={dependencies} violation={{
      id: 'cycle-1', severity: 'HIGH', ruleId: 'no-cycles', fromModule: 'pkg.a', toModule: 'pkg.b',
      blastRadiusCount: 3, affectedModules: ['pkg.consumer'], explanation: 'Cycle found.', explanationFallback: false
    }} />);

    expect(screen.getByText('Packages in cycle (3)')).toBeTruthy();
    expect(screen.getByText('pkg.a').closest('ul').textContent).toBe('pkg.apkg.bpkg.c');
    expect(screen.getByText('Dependencies inside the cycle: 3')).toBeTruthy();
    expect(screen.queryByText('pkg.consumer →')).toBeNull();
  });
});