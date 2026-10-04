import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App.jsx';
import ResultsPage from './components/ResultsPage.jsx';

vi.mock('cytoscape', () => ({
  default: vi.fn(() => ({
    destroy: vi.fn(),
    edges: vi.fn(() => ({ length: 0, forEach: vi.fn() })),
    elements: vi.fn(() => []),
    fit: vi.fn(),
    layout: vi.fn(() => ({ run: vi.fn() })),
    nodes: vi.fn(() => []),
    on: vi.fn()
  }))
}));

const scan = {
  id: 'scan-1', repositoryId: 'repository-1', repositoryUrl: 'https://github.com/acme/example.git',
  status: 'COMPLETED', healthScore: 80
};

const graph = {
  modules: [
    { id: 'com.example.web', layer: 'web', cycleMember: false },
    { id: 'com.example.data', layer: 'repository', cycleMember: false }
  ],
  dependencies: [{ id: 'edge-1', from: 'com.example.web', to: 'com.example.data', violationIds: ['violation-1'] }]
};

const violations = [{
  id: 'violation-1', ruleId: 'forbidden:web->repository', fromModule: 'com.example.web',
  toModule: 'com.example.data', blastRadiusCount: 2, explanation: 'Web depends on data.', explanationFallback: true
}];

afterEach(() => vi.restoreAllMocks());

describe('dashboard', () => {
  it('submits a scan, polls it, and renders completed results', async () => {
    const queuedScan = { ...scan, status: 'QUEUED', healthScore: null };
    const fetch = vi.fn()
      .mockResolvedValueOnce(response(queuedScan, 202))
      .mockResolvedValueOnce(response(scan))
      .mockResolvedValueOnce(response(graph))
      .mockResolvedValueOnce(response(violations))
      .mockResolvedValueOnce(response([scan]))
      .mockResolvedValueOnce(response({ custom: false, rulesYaml: null, rules: { layers: [], forbidden: [], noCycles: true } }));
    vi.stubGlobal('fetch', fetch);

    render(<App />);
    fireEvent.change(screen.getByLabelText('Repository URL'), { target: { value: scan.repositoryUrl } });
    fireEvent.click(screen.getByRole('button', { name: 'Start scan' }));

    await waitFor(() => expect(screen.getByText(scan.repositoryUrl)).toBeTruthy(), { timeout: 2_000 });
    expect(fetch.mock.calls.map(([url]) => url)).toEqual([
      '/api/scans', '/api/scans/scan-1', '/api/scans/scan-1/graph',
      '/api/scans/scan-1/violations', '/api/repos/repository-1/history', '/api/scans/scan-1/rules'
    ]);
  });

  it('filters packages and shows selected violation details', () => {
    render(<ResultsPage scan={scan} results={{ graph, violations, history: [scan] }} onStartOver={vi.fn()} />);

    expect(screen.getByText('Architecture Health').parentElement.textContent).toContain('80/100');
    expect(screen.getByText('Packages analyzed').parentElement.textContent).toContain('2');
    expect(screen.getByText('Dependencies found').parentElement.textContent).toContain('1');
    expect(screen.getByText('Cycles detected').parentElement.textContent).toContain('0');
    expect(screen.getByText('Architecture violations').parentElement.textContent).toContain('1');
    expect(screen.queryByText('Parse/analyzer warnings')).toBeNull();

    fireEvent.change(screen.getByPlaceholderText('com.example'), { target: { value: 'data' } });
    expect(screen.getByText('Showing 1 of 2 nodes')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: /forbidden:web->repository/i }));
    expect(screen.getByText('Web depends on data.')).toBeTruthy();
    expect(screen.getByText('Fallback explanation (AI service was unavailable)')).toBeTruthy();
  });

  it('renders parse warnings when the scan provides them', () => {
    render(<ResultsPage scan={{ ...scan, parseFailureCount: 3 }} results={{ graph, violations, history: [scan] }} onStartOver={vi.fn()} />);

    expect(screen.getByText('Parse/analyzer warnings').parentElement.textContent).toContain('3');
  });

  it('shows rule matches, zero-match warnings, and unknown package counts', () => {
    render(<ResultsPage scan={scan} results={{
      graph: { modules: [...graph.modules, { id: 'com.example.util', layer: 'unknown', cycleMember: false }], dependencies: graph.dependencies },
      violations, history: [scan],
      rules: {
        custom: true,
        rulesYaml: 'layers:\n  - name: web\n    packagePatterns: [com.example.web]\nnoCycles: true\n',
        rules: { layers: [{ name: 'web', packagePatterns: ['com.example.web'] }, { name: 'service', packagePatterns: ['com.example.service'] }], forbidden: [], noCycles: true }
      }
    }} onStartOver={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /Rules used/i }));
    expect(screen.getByText('1 packages matched')).toBeTruthy();
    expect(screen.getByText('Matches no packages. Check the prefix.')).toBeTruthy();
    expect(screen.getByText('1 packages are not in any layer, so layer rules do not apply to them.')).toBeTruthy();
  });

  it('resets hidden filters and clears a violation when it is selected again', () => {
    render(<ResultsPage scan={scan} results={{ graph, violations, history: [scan] }} onStartOver={vi.fn()} />);

    fireEvent.change(screen.getByPlaceholderText('com.example'), { target: { value: 'data' } });
    fireEvent.click(screen.getByRole('button', { name: /View on graph/i }));
    expect(screen.getByPlaceholderText('com.example').value).toBe('');
    expect(screen.getByLabelText('Layer').value).toBe('all');
    expect(screen.getByText('Web depends on data.')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /View on graph/i }));
    expect(screen.getByText('Select a violation to focus its dependency and view its analysis.')).toBeTruthy();
  });
});

function response(body, status = 200) {
  return { ok: status >= 200 && status < 300, status, json: () => Promise.resolve(body) };
}
