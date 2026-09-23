function scoreClass(score) {
  if (score >= 80) return 'healthy';
  if (score >= 50) return 'watch';
  return 'risk';
}

export default function HealthTrend({ history }) {
  const completed = history.filter((scan) => Number.isInteger(scan.healthScore)).slice().reverse();
  if (!completed.length) return null;

  return <section className="health-trend panel">
    <div className="trend-header"><div><p className="eyebrow">HEALTH TREND</p><h3>Architecture health</h3></div><strong className={`score ${scoreClass(completed[completed.length - 1].healthScore)}`}>{completed[completed.length - 1].healthScore}/100</strong></div>
    <div className="trend-chart" aria-label="Health score trend">
      {completed.map((scan) => <div className="trend-point" key={scan.id} title={`${scan.createdAt}: ${scan.healthScore}/100`}>
        <div className={`trend-bar ${scoreClass(scan.healthScore)}`} style={{ height: `${Math.max(4, scan.healthScore)}%` }} />
        <small>{scan.healthScore}</small>
      </div>)}
    </div>
    <p className="trend-help">Scores deduct 10 points per deterministic violation, with a floor of 0.</p>
  </section>;
}
