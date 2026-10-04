import { useEffect, useState } from 'react';
import { cancelScan, createScan, getGraph, getRepositoryHistory, getScan, getScanRules, getViolations } from './api.js';
import { emptyModel } from './rulesBuilder.js';
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
  const [viewHistory, setViewHistory] = useState(['form']);
  const [formOptions, setFormOptions] = useState({});

  function navigate(nextView) {
    if (view === nextView) return;
    setViewHistory((currentHistory) => [...currentHistory, nextView]);
    setView(nextView);
  }

  function goBack() {
    setViewHistory((currentHistory) => {
      if (currentHistory.length < 2) return currentHistory;
      const nextHistory = currentHistory.slice(0, -1);
      setView(nextHistory[nextHistory.length - 1]);
      return nextHistory;
    });
  }

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
    Promise.all([getGraph(scan.id), getViolations(scan.id), getRepositoryHistory(scan.repositoryId), getScanRules(scan.id)])
      .then(([graph, violations, history, rules]) => setResults({ graph, violations, history, rules }))
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoadingResults(false));
  }, [scan?.id, scan?.status]);

  async function submit(values) {
    setSubmitting(true); setError(null); setResults(null); setLoadingResults(false); setFormOptions({});
    try { setScan(await createScan(values)); navigate('status'); } catch (requestError) { setError(requestError.message); }
    finally { setSubmitting(false); }
  }

  function startOver() { setScan(null); setResults(null); setError(null); setLoadingResults(false); setFormOptions({}); setView('form'); setViewHistory(['form']); }
  function openResults(selectedScan) { setScan(selectedScan); setResults(null); setError(null); navigate('status'); }

  return <main className="app-shell">
    <header><p className="eyebrow">ARCHITECTURE OBSERVABILITY</p><h1>ArchGuard</h1><p>Static facts first. Clear explanations second.</p>
      <nav className="app-nav" aria-label="Scan views">{viewHistory.length > 1 && <button className="secondary" onClick={goBack}>Back</button>}<button className="secondary" aria-pressed={view === 'queue'} onClick={() => navigate('queue')}>Scan queue</button>
        <button className="secondary" aria-pressed={view === 'form'} onClick={startOver}>New scan</button></nav></header>
    {error && <div className="error-banner" role="alert">{error}<button onClick={() => setError(null)}>Dismiss</button></div>}
    {view === 'queue' && <ScanQueueDashboard onOpenResults={openResults} onScanCreated={(createdScan) => { setScan(createdScan); setResults(null); navigate('status'); }} />}
    {view === 'form' && <ScanForm key={formOptions.initialRepoUrl || 'new'} onSubmit={submit} submitting={submitting} {...formOptions} />}
    {view === 'status' && scan && !results && <ScanStatus scan={scan} loadingResults={loadingResults} onStartOver={startOver} onCancel={async () => setScan(await cancelScan(scan.id))} />}
    {view === 'status' && results && <ResultsPage scan={scan} results={results} onStartOver={startOver} onEditRules={() => {
      const rules = results.rules?.custom && results.rules.rules ? results.rules.rules : emptyModel();
      setFormOptions({ initialRepoUrl: scan.repositoryUrl, initialModel: rules, initialRawYaml: results.rules?.rulesYaml || '', initialTab: 'guided', packageModules: results.graph.modules });
      navigate('form');
    }} onRescan={() => submit({ repoUrl: scan.repositoryUrl, rulesYaml: results.rules?.rulesYaml || '' })} />}
  </main>;
}
