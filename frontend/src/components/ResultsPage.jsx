import { useCallback, useMemo, useRef, useState } from 'react';
import DependencyGraph from './DependencyGraph.jsx';
import ViolationList from './ViolationList.jsx';
import ViolationDetails from './ViolationDetails.jsx';
import HealthTrend from './HealthTrend.jsx';
import { findCycles } from '../graphUtils.js';

function scoreClass(score) {
  if (score >= 80) return 'healthy';
  if (score >= 50) return 'watch';
  return 'risk';
}

export default function ResultsPage({ scan, results, onStartOver }) {
  const [selectedViolation, setSelectedViolation] = useState(null);
  const [selectedNode, setSelectedNode] = useState(null);
  const [search, setSearch] = useState('');
  const [layer, setLayer] = useState('all');
  const graphViewportRef = useRef(null);
  const layers = useMemo(() => ['all', ...new Set(results.graph.modules.map((node) => node.layer))], [results.graph]);
  const visibleNodes = useMemo(() => results.graph.modules.filter((node) => (layer === 'all' || node.layer === layer) && node.id.toLowerCase().includes(search.toLowerCase())), [results.graph.modules, layer, search]);
  const handleNodeSelect = useCallback((nodeId) => setSelectedNode(nodeId), []);
  const handleViolationSelect = useCallback((violation) => {
    if (selectedViolation?.id === violation.id) {
      setSelectedViolation(null);
      return;
    }
    setSelectedViolation(violation);
    setSearch('');
    setLayer('all');
    if (window.innerWidth <= 850) graphViewportRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, [selectedViolation]);
  const handleGraphBackgroundSelect = useCallback(() => setSelectedViolation(null), []);
  const cycles = findCycles(results.graph.modules, results.graph.dependencies);
  const metrics = [
    { label: 'Architecture Health', value: `${scan.healthScore}/100`, className: scoreClass(scan.healthScore) },
    { label: 'Packages analyzed', value: results.graph.modules.length },
    { label: 'Dependencies found', value: results.graph.dependencies.length },
    { label: 'Cycles detected', value: cycles.length },
    { label: 'Architecture violations', value: results.violations.length },
    ...(typeof scan.parseFailureCount === 'number' ? [{ label: 'Parse/analyzer warnings', value: scan.parseFailureCount }] : [])
  ];
  return <section className="results"><div className="results-header"><div><p className="eyebrow">SCAN COMPLETE</p><h2>{scan.repositoryUrl}</h2></div><div className="results-actions"><button className="secondary" onClick={onStartOver}>New scan</button></div></div>
    <section className="scan-summary" aria-label="Scan result"><p className="eyebrow">SCAN RESULT</p><div className="metric-grid">{metrics.map((metric) => <div className="metric-tile" key={metric.label}><span>{metric.label}</span><strong className={metric.className}>{metric.value}</strong></div>)}</div></section>
    <HealthTrend history={results.history} />
    <div className="toolbar"><label>Search packages<input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="com.example" /></label><label>Layer<select value={layer} onChange={(event) => setLayer(event.target.value)}>{layers.map((value) => <option key={value}>{value}</option>)}</select></label><p>Showing {visibleNodes.length} of {results.graph.modules.length} nodes</p></div>
    <div className="dashboard-grid"><div className="graph-column" ref={graphViewportRef}><DependencyGraph graph={results.graph} visibleNodes={visibleNodes} selectedViolation={selectedViolation} selectedNode={selectedNode} onNodeSelect={handleNodeSelect} onBackgroundSelect={handleGraphBackgroundSelect} /></div>
      <aside><ViolationList violations={results.violations} modules={results.graph.modules} dependencies={results.graph.dependencies} selectedId={selectedViolation?.id} onSelect={handleViolationSelect} /><ViolationDetails violation={selectedViolation} dependencies={results.graph.dependencies} /></aside></div>
  </section>;
}
