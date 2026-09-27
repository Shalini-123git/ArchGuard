import { useEffect, useState } from 'react';
import { getQueueStatus, getScans } from '../api.js';
import { SCAN_QUEUE_POLL_INTERVAL_MS, SCAN_STUCK_THRESHOLD_MS } from '../config.js';

const PAGE_SIZE = 50;
const ACTIVE_STATUSES = ['QUEUED', 'RUNNING'];
const STATUSES = ['', ...ACTIVE_STATUSES, 'COMPLETED', 'FAILED'];

export default function ScanQueueDashboard({ onOpenResults, pollIntervalMs = SCAN_QUEUE_POLL_INTERVAL_MS,
  stuckThresholdMs = SCAN_STUCK_THRESHOLD_MS }) {
  const [scans, setScans] = useState([]);
  const [queue, setQueue] = useState(null);
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    async function refresh() {
      try {
        const [scanList, queueStatus] = await Promise.all([
          getScans({ status, page, size: PAGE_SIZE }), getQueueStatus()
        ]);
        if (active) {
          setScans(scanList);
          setQueue(queueStatus);
          setError(null);
        }
      } catch (requestError) {
        if (active) setError(requestError.message);
      }
    }
    refresh();
    const timer = window.setInterval(refresh, pollIntervalMs);
    return () => { active = false; window.clearInterval(timer); };
  }, [status, page, pollIntervalMs]);

  const now = Date.now();
  const hasNextPage = scans.length === PAGE_SIZE;

  return <section className="panel scan-queue" aria-labelledby="scan-queue-title">
    <div className="queue-heading"><div><p className="eyebrow">LIVE OPERATIONS</p><h2 id="scan-queue-title">Scan queue</h2></div>
      <div className="queue-summary" aria-live="polite">
        <strong>{queue?.runningScans ?? '—'} running</strong>
        <span>{queue?.queuedScans ?? '—'} queued</span>
        <span>{queue?.maxConcurrentScans ?? '—'} max concurrent</span>
      </div>
    </div>
    {error && <p className="queue-error" role="alert">{error}</p>}
    <div className="queue-toolbar">
      <label htmlFor="scan-status-filter">Status</label>
      <select id="scan-status-filter" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}>
        {STATUSES.map((value) => <option key={value || 'all'} value={value}>{value || 'All scans'}</option>)}
      </select>
    </div>
    <div className="scan-table-wrap">
      <table className="scan-table">
        <thead><tr><th scope="col">ID</th><th scope="col">Source</th><th scope="col">Status</th><th scope="col">Created</th><th scope="col">Updated</th><th scope="col">Results</th></tr></thead>
        <tbody>
          {scans.map((scan) => {
            const active = ACTIVE_STATUSES.includes(scan.status);
            const activityAt = scan.status === 'RUNNING' ? scan.startedAt : scan.createdAt;
            const stuck = active && activityAt && now - new Date(activityAt).getTime() > stuckThresholdMs;
            return <tr key={scan.id} className={stuck ? 'scan-row-stuck' : undefined}>
              <td title={scan.id} className="scan-id">{scan.id.slice(0, 8)}…</td>
              <td className="scan-source" title={displaySource(scan.repositoryUrl)}>{displaySource(scan.repositoryUrl)}</td>
              <td><span className={`status ${scan.status.toLowerCase()}`}>{scan.status}</span>{stuck && <span className="stuck-label">Long-running</span>}</td>
              <td>{formatTimestamp(scan.createdAt)}</td>
              <td>{formatTimestamp(scan.completedAt || scan.startedAt || scan.createdAt)}</td>
              <td>{scan.status === 'COMPLETED'
                ? <a className="results-link" href={`#scan-${scan.id}`} onClick={(event) => { event.preventDefault(); onOpenResults(scan); }}>View results</a>
                : '—'}</td>
            </tr>;
          })}
          {scans.length === 0 && <tr><td className="empty-scans" colSpan="6">No scans on this page.</td></tr>}
        </tbody>
      </table>
    </div>
    <div className="queue-pagination"><button className="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Newer</button>
      <span>Page {page + 1}</span><button className="secondary" disabled={!hasNextPage} onClick={() => setPage(page + 1)}>Older</button></div>
  </section>;
}

function displaySource(source) {
  return source?.startsWith('local:') ? source.slice('local:'.length) : source;
}

function formatTimestamp(value) {
  return value ? new Date(value).toLocaleString() : '—';
}