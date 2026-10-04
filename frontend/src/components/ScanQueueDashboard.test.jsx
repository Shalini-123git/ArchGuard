import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ScanQueueDashboard from './ScanQueueDashboard.jsx';

afterEach(() => vi.restoreAllMocks());

describe('scan queue dashboard', () => {
  it('shows live queue state, flags old active scans, and opens completed results', async () => {
    const queued = {
      id: 'queued-123456789', repositoryUrl: 'local:C:\\workspace\\sample', status: 'RUNNING',
      createdAt: new Date(Date.now() - 180_000).toISOString(),
      startedAt: new Date(Date.now() - 150_000).toISOString()
    };
    const completed = {
      id: 'completed-123456', repositoryUrl: 'https://github.com/acme/sample.git', status: 'COMPLETED',
      createdAt: new Date().toISOString(), completedAt: new Date().toISOString()
    };
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(response([queued, completed]))
      .mockResolvedValueOnce(response({ runningScans: 1, queuedScans: 2, maxConcurrentScans: 4 })));
    const onOpenResults = vi.fn();

    render(<ScanQueueDashboard onOpenResults={onOpenResults} pollIntervalMs={60_000} stuckThresholdMs={120_000} />);

    expect(await screen.findByText('Long-running')).toBeTruthy();
    expect(screen.getByText('1 running')).toBeTruthy();
    expect(screen.getByText('2 queued')).toBeTruthy();
    expect(screen.getByText('4 max concurrent')).toBeTruthy();
    expect(screen.getByText('C:\\workspace\\sample')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'View results' }));
    await waitFor(() => expect(onOpenResults).toHaveBeenCalledWith(completed));
  });

  it('confirms cancellation for active queue rows', async () => {
    const running = { id: 'running-123456', repositoryUrl: 'https://github.com/acme/sample.git', status: 'RUNNING', createdAt: new Date().toISOString() };
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(response([running]))
      .mockResolvedValueOnce(response({ runningScans: 1, queuedScans: 0, maxConcurrentScans: 4 }))
      .mockResolvedValueOnce(response({ ...running, status: 'CANCELLED', errorMessage: 'Cancelled by user' })));
    render(<ScanQueueDashboard onOpenResults={vi.fn()} pollIntervalMs={60_000} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Cancel' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancel this scan? Partial results are discarded.' }));
    await waitFor(() => expect(screen.getByText('CANCELLED')).toBeTruthy());
  });

  it('expands failed error text, filters repositories, and formats duration', async () => {
    const now = Date.now();
    const failed = { id: 'failed-123456', repositoryUrl: 'https://github.com/acme/failed.git', status: 'FAILED',
      errorMessage: 'A very long failure message that explains exactly why this scan failed and contains more details than the collapsed row should show to users.',
      createdAt: new Date(now - 120_000).toISOString(), startedAt: new Date(now - 90_000).toISOString(),
      completedAt: new Date(now - 30_000).toISOString() };
    const other = { id: 'other-123456', repositoryUrl: 'https://github.com/acme/other.git', status: 'COMPLETED',
      createdAt: new Date(now).toISOString(), startedAt: new Date(now - 10_000).toISOString(), completedAt: new Date(now).toISOString(), healthScore: 90 };
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(response([failed, other]))
      .mockResolvedValueOnce(response({ runningScans: 0, queuedScans: 0, maxConcurrentScans: 2 })));
    render(<ScanQueueDashboard onOpenResults={vi.fn()} pollIntervalMs={60_000} />);
    expect(await screen.findByText('1m 0s')).toBeTruthy();
    expect(screen.getByText(/A very long failure message/)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Expand' }));
    expect(screen.getByText(failed.errorMessage)).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Search repository'), { target: { value: 'other' } });
    expect(screen.queryByText('acme/failed')).toBeNull();
    expect(screen.getByText('acme/other')).toBeTruthy();
  });

  it('re-runs a finished scan with its stored rules', async () => {
    const scan = { id: 'failed-123456', repositoryUrl: 'https://github.com/acme/failed.git', status: 'FAILED', createdAt: new Date().toISOString() };
    const fetch = vi.fn()
      .mockResolvedValueOnce(response([scan]))
      .mockResolvedValueOnce(response({ runningScans: 0, queuedScans: 0, maxConcurrentScans: 2 }))
      .mockResolvedValueOnce(response({ custom: true, rulesYaml: 'noCycles: false\n' }))
      .mockResolvedValueOnce(response({ id: 'new-scan', status: 'QUEUED' }));
    vi.stubGlobal('fetch', fetch);
    const onScanCreated = vi.fn();
    render(<ScanQueueDashboard onOpenResults={vi.fn()} onScanCreated={onScanCreated} pollIntervalMs={60_000} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Re-run' }));
    await waitFor(() => expect(onScanCreated).toHaveBeenCalledWith({ id: 'new-scan', status: 'QUEUED' }));
    expect(fetch).toHaveBeenLastCalledWith('/api/scans', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ repoUrl: scan.repositoryUrl, rulesYaml: 'noCycles: false\n' })
    }));
  });
});

function response(body) {
  return { ok: true, status: 200, json: () => Promise.resolve(body) };
}