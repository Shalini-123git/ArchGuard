import { useEffect, useState } from 'react';
import { createScan, getGraph, getRepositoryHistory, getScan, getViolations } from './api.js';
import ScanForm from './components/ScanForm.jsx';
import ScanStatus from './components/ScanStatus.jsx';
import ResultsPage from './components/ResultsPage.jsx';
import ScanQueueDashboard from './components/ScanQueueDashboard.jsx';

export default function App() {
  const [scan, setScan] = useState(null);
  const [results, setResults] = useState(null);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [loadingResults, setLoadingResults] = useState(false);
  const [view, setView] = useState('form');

  useEffect(() => {
    if (!scan || !['QUEUED', 'RUNNING'].includes(scan.status)) return undefined;
    const timer = window.setTimeout(async () => {
      try { setScan(await getScan(scan.id)); } catch (requestError) { setError(requestError.message); }
    }, 1000);
    return () => window.clearTimeout(timer);
  }, [scan]);

  useEffect(() => {
    if (scan?.status !== 'COMPLETED') return;
    setLoadingResults(true);
    Promise.all([getGraph(scan.id), getViolations(scan.id), getRepositoryHistory(scan.repositoryId)])
      .then(([graph, violations, history]) => setResults({ graph, violations, history }))
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoadingResults(false));
  }, [scan?.id, scan?.status]);

  async function submit(values) {
    setSubmitting(true); setError(null); setResults(null); setLoadingResults(false);
    try { setScan(await createScan(values)); setView('status'); } catch (requestError) { setError(requestError.message); }
    finally { setSubmitting(false); }
  }

  function startOver() { setScan(null); setResults(null); setError(null); setLoadingResults(false); setView('form'); }
  function openResults(selectedScan) { setScan(selectedScan); setResults(null); setError(null); setView('status'); }

  return <main className="app-shell">
    <header><p className="eyebrow">ARCHITECTURE OBSERVABILITY</p><h1>ArchGuard</h1><p>Static facts first. Clear explanations second.</p>
      <nav className="app-nav" aria-label="Scan views"><button className="secondary" aria-pressed={view === 'queue'} onClick={() => setView('queue')}>Scan queue</button>
        <button className="secondary" aria-pressed={view === 'form'} onClick={startOver}>New scan</button></nav></header>
    {error && <div className="error-banner" role="alert">{error}<button onClick={() => setError(null)}>Dismiss</button></div>}
    {view === 'queue' && <ScanQueueDashboard onOpenResults={openResults} />}
    {view === 'form' && <ScanForm onSubmit={submit} submitting={submitting} />}
    {view === 'status' && scan && !results && <ScanStatus scan={scan} loadingResults={loadingResults} onStartOver={startOver} />}
    {view === 'status' && results && <ResultsPage scan={scan} results={results} onStartOver={startOver} />}
  </main>;
}
