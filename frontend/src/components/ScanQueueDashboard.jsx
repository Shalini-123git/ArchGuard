import { useEffect, useMemo, useState } from 'react';
import { cancelScan, createScan, getQueueStatus, getScanRules, getScans } from '../api.js';
import { SCAN_QUEUE_POLL_INTERVAL_MS, SCAN_STUCK_THRESHOLD_MS } from '../config.js';

const PAGE_SIZE = 50;
const ACTIVE_STATUSES = ['QUEUED', 'RUNNING'];
const FINISHED_STATUSES = ['COMPLETED', 'FAILED', 'CANCELLED'];
const STATUSES = ['', ...ACTIVE_STATUSES, ...FINISHED_STATUSES];

export default function ScanQueueDashboard({ onOpenResults, onScanCreated, pollIntervalMs = SCAN_QUEUE_POLL_INTERVAL_MS,
  stuckThresholdMs = SCAN_STUCK_THRESHOLD_MS }) {
  const [scans, setScans] = useState([]);
  const [queue, setQueue] = useState(null);
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [error, setError] = useState(null);
  const [confirmingId, setConfirmingId] = useState(null);
  const [expandedErrors, setExpandedErrors] = useState(new Set());
  const [rerunningId, setRerunningId] = useState(null);

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

  const filteredScans = useMemo(() => {
    const query = search.trim().toLowerCase();
    return query ? scans.filter((scan) => displaySource(scan.repositoryUrl).toLowerCase().includes(query)) : scans;
  }, [scans, search]);
  const counts = useMemo(() => Object.fromEntries(STATUSES.map((value) => [
    value, value ? scans.filter((scan) => scan.status === value).length : scans.length
  ])), [scans]);
  const now = Date.now();
  const hasNextPage = scans.length === PAGE_SIZE;

  async function cancel(id) {
    if (confirmingId !== id) { setConfirmingId(id); return; }
    try {
      const updated = await cancelScan(id);
      setScans((current) => current.map((scan) => scan.id === id ? updated : scan));
      setConfirmingId(null);
    } catch (requestError) { setError(requestError.message); }
  }

  async function rerun(scan) {
    setRerunningId(scan.id);
    try {
      const rules = await getScanRules(scan.id);
      const created = await createScan({ repoUrl: scan.repositoryUrl, rulesYaml: rules.custom ? rules.rulesYaml : '' });
      onScanCreated?.(created);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setRerunningId(null);
    }
  }

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
      <label htmlFor="scan-search">Search repository</label>
      <input id="scan-search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="owner/name" />
      <label htmlFor="scan-status-filter">Status</label>
      <select id="scan-status-filter" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}>
        {STATUSES.map((value) => <option key={value || 'all'} value={value}>{statusLabel(value, counts[value])}</option>)}
      </select>
    </div>
    <div className="scan-table-wrap">
      <table className="scan-table">
        <thead><tr><th scope="col">Repository</th><th scope="col">Status</th><th scope="col">Created</th><th scope="col">Duration</th><th scope="col">Health</th><th scope="col">Actions</th></tr></thead>
        <tbody>
          {filteredScans.map((scan) => <ScanRow key={scan.id} scan={scan} now={now} stuckThresholdMs={stuckThresholdMs}
            expanded={expandedErrors.has(scan.id)} onToggleError={() => setExpandedErrors((current) => {
              const next = new Set(current);
              if (next.has(scan.id)) next.delete(scan.id); else next.add(scan.id);
              return next;
            })} confirming={confirmingId === scan.id} onCancel={() => cancel(scan.id)}
            rerunning={rerunningId === scan.id} onRerun={() => rerun(scan)} onOpenResults={onOpenResults} />)}
          {filteredScans.length === 0 && <tr><td className="empty-scans" colSpan="6">No scans on this page.</td></tr>}
        </tbody>
      </table>
    </div>
    <div className="queue-pagination"><button className="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Newer</button>
      <span>Page {page + 1}</span><button className="secondary" disabled={!hasNextPage} onClick={() => setPage(page + 1)}>Older</button></div>
  </section>;
}

function ScanRow({ scan, now, stuckThresholdMs, expanded, onToggleError, confirming, onCancel, rerunning, onRerun, onOpenResults }) {
  const active = ACTIVE_STATUSES.includes(scan.status);
  const activityAt = scan.status === 'RUNNING' ? scan.startedAt : scan.createdAt;
  const stuck = active && activityAt && now - new Date(activityAt).getTime() > stuckThresholdMs;
  return <tr className={stuck ? 'scan-row-stuck' : undefined}>
    <td data-label="Repository" className="scan-source" title={scan.repositoryUrl}>{repositoryName(scan.repositoryUrl)}</td>
    <td data-label="Status"><span className={`status ${scan.status.toLowerCase()}`}>{scan.status}</span>{stuck && <span className="stuck-label">Long-running</span>}
      {scan.status === 'FAILED' && scan.errorMessage && <div className="scan-error"><span className="scan-error-text">{expanded ? scan.errorMessage : truncate(scan.errorMessage)}</span>{scan.errorMessage.length > 90 && <button type="button" className="text-button" onClick={onToggleError}>{expanded ? 'Collapse' : 'Expand'}</button>}</div>}</td>
    <td data-label="Created">{formatTimestamp(scan.createdAt)}</td>
    <td data-label="Duration">{formatDuration(scan, now)}</td>
    <td data-label="Health">{scan.status === 'COMPLETED' ? <span className={`health-score ${healthClass(scan.healthScore)}`}>{scan.healthScore ?? '—'}</span> : '—'}</td>
    <td data-label="Actions" className="scan-actions">{scan.status === 'COMPLETED' && <button type="button" className="secondary small-button" onClick={() => onOpenResults(scan)}>View results</button>}
      {active && <button type="button" className="secondary small-button" onClick={onCancel}>{confirming ? 'Cancel this scan? Partial results are discarded.' : 'Cancel'}</button>}
      {FINISHED_STATUSES.includes(scan.status) && <button type="button" className="secondary small-button" disabled={rerunning} onClick={onRerun}>{rerunning ? 'Re-running…' : 'Re-run'}</button>}</td>
  </tr>;
}

function statusLabel(status, count) {
  if (!status) return `All scans (${count})`;
  return `${status[0]}${status.slice(1).toLowerCase()} (${count})`;
}

function repositoryName(source) {
  if (source?.startsWith('local:')) return displaySource(source);
  const value = displaySource(source);
  try {
    const path = new URL(value).pathname.replace(/^\/|\/$/g, '').replace(/\.git$/, '');
    return path || value;
  } catch {
    return value;
  }
}

function displaySource(source) {
  return source?.startsWith('local:') ? source.slice('local:'.length) : source || '—';
}

function formatTimestamp(value) {
  return value ? new Date(value).toLocaleString() : '—';
}

export function formatDuration(scan, now = Date.now()) {
  const start = scan.startedAt && new Date(scan.startedAt).getTime();
  const end = scan.completedAt && new Date(scan.completedAt).getTime();
  if (!start) return '—';
  return formatElapsed((end || now) - start, !end);
}

function formatElapsed(milliseconds, running) {
  const seconds = Math.max(0, Math.floor(milliseconds / 1000));
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  const value = minutes ? `${minutes}m ${remainingSeconds}s` : `${remainingSeconds}s`;
  return running ? `running for ${value}` : value;
}

function truncate(value) {
  return value.length > 90 ? `${value.slice(0, 87)}…` : value;
}

function healthClass(score) {
  if (score >= 80) return 'healthy';
  if (score >= 50) return 'watch';
  return 'risk';
}
