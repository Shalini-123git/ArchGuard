import { useMemo, useState } from 'react';
import DependencyGraph from './DependencyGraph.jsx';
import ViolationList from './ViolationList.jsx';
import ViolationDetails from './ViolationDetails.jsx';
import HealthTrend from './HealthTrend.jsx';

export default function ResultsPage({ scan, results, onStartOver }) {
  const [selectedViolation, setSelectedViolation] = useState(null);
  const [selectedNode, setSelectedNode] = useState(null);
  const [search, setSearch] = useState('');
  const [layer, setLayer] = useState('all');
  const layers = useMemo(() => ['all', ...new Set(results.graph.modules.map((node) => node.layer))], [results.graph]);
  const visibleNodes = results.graph.modules.filter((node) => (layer === 'all' || node.layer === layer) && node.id.toLowerCase().includes(search.toLowerCase()));
  return <section className="results"><div className="results-header"><div><p className="eyebrow">SCAN COMPLETE</p><h2>{scan.repositoryUrl}</h2></div><div className="results-actions"><strong className="current-score">Health {scan.healthScore}/100</strong><button className="secondary" onClick={onStartOver}>New scan</button></div></div>
    <HealthTrend history={results.history} />
    <div className="toolbar"><label>Search packages<input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="com.example" /></label><label>Layer<select value={layer} onChange={(event) => setLayer(event.target.value)}>{layers.map((value) => <option key={value}>{value}</option>)}</select></label><p>Showing {visibleNodes.length} of {results.graph.modules.length} nodes</p></div>
    <div className="dashboard-grid"><DependencyGraph graph={results.graph} visibleNodes={visibleNodes} selectedViolation={selectedViolation} selectedNode={selectedNode} onNodeSelect={setSelectedNode} />
      <aside><ViolationList violations={results.violations} selectedId={selectedViolation?.id} onSelect={setSelectedViolation} /><ViolationDetails violation={selectedViolation} /></aside></div>
  </section>;
}
