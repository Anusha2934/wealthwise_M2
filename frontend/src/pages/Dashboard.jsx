import { useState, useEffect } from 'react';
import { fetchSchemeSummary } from '../api';
import { classifyAll } from '../api';

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [classifying, setClassifying] = useState(false);
  const [classifyMsg, setClassifyMsg] = useState('');

  useEffect(() => {
    fetchSchemeSummary().then(setSummary).catch(() => setSummary({}));
  }, []);

  async function handleClassify() {
    setClassifying(true);
    setClassifyMsg('');
    try {
      const msg = await classifyAll();
      setClassifyMsg(msg || 'Classification complete.');
    } catch {
      setClassifyMsg('Failed. Is the backend running?');
    } finally {
      setClassifying(false);
    }
  }

  return (
    <div>
      <p className="page-title">Dashboard</p>
      <p className="page-sub">Live overview of your WealthWise backend data</p>

      {!summary ? (
        <div className="spinner" />
      ) : (
        <>
          <div className="stat-grid">
            <StatCard label="Total Schemes" value={summary.totalSchemes?.toLocaleString() ?? '—'} sub="in scheme_master" color="blue" icon="📊" />
            <StatCard label="Direct Plans" value={summary.directPlans?.toLocaleString() ?? '—'} sub="expense-optimised" color="green" icon="✅" />
            <StatCard label="Regular Plans" value={summary.regularPlans?.toLocaleString() ?? '—'} sub="distributor plans" color="yellow" icon="🔄" />
            <StatCard label="NAV Records" value={summary.navRecords?.toLocaleString() ?? '—'} sub="historical prices" color="blue" icon="📈" />
            <StatCard label="AMC Count" value={summary.amcCount?.toLocaleString() ?? '—'} sub="fund houses" color="" icon="🏦" />
            <StatCard label="Active Schemes" value={summary.activeSchemes?.toLocaleString() ?? '—'} sub="open for investment" color="green" icon="🟢" />
          </div>

          <div className="card" style={{ maxWidth: 540 }}>
            <div className="section-header">
              <span className="section-title">⚙️ Admin Actions</span>
            </div>
            <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 14 }}>
              Run the SEBI category classifier on all schemes. This tags each fund with a broad category (EQUITY / DEBT / HYBRID) and risk level.
            </p>
            <button className="btn btn-primary" onClick={handleClassify} disabled={classifying}>
              {classifying ? 'Classifying…' : '🏷️ Run SEBI Classifier'}
            </button>
            {classifyMsg && (
              <div className="alert alert-success" style={{ marginTop: 12, marginBottom: 0 }}>
                ✅ {classifyMsg}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}

function StatCard({ label, value, sub, color, icon }) {
  return (
    <div className="stat-card">
      <div className="stat-label">{icon} {label}</div>
      <div className={`stat-value ${color}`}>{value}</div>
      <div className="stat-sub">{sub}</div>
    </div>
  );
}
