import { useState, useEffect } from 'react';
import { fetchPortfolioReport } from '../api';
import { fmt, pct, gainColor, Badge, categoryBadgeType, RiskPips, PieChart } from '../utils.jsx';

export default function Portfolio({ userId }) {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [inputId, setInputId] = useState(userId);

  async function load(uid) {
    setLoading(true); setError('');
    try {
      const data = await fetchPortfolioReport(uid);
      if (data.message) { setError(data.message); setReport(null); }
      else setReport(data);
    } catch { setError('Could not fetch portfolio.'); }
    setLoading(false);
  }

  useEffect(() => { load(userId); }, [userId]);

  const alloc = report?.allocationByCategory
    ? Object.entries(report.allocationByCategory).map(([label, value]) => ({
        label, value: parseFloat(value)
      }))
    : [];

  return (
    <div>
      <p className="page-title">Portfolio</p>
      <p className="page-sub">Holdings, allocation, and unrealised gains</p>

      <div className="search-bar" style={{ marginBottom: 20, maxWidth: 400 }}>
        <input
          className="input input-grow"
          type="number"
          min="1"
          placeholder="Enter User ID"
          value={inputId}
          onChange={e => setInputId(e.target.value)}
        />
        <button className="btn btn-primary" onClick={() => load(inputId)}>Load</button>
      </div>

      {loading && <div className="spinner" />}

      {error && !loading && (
        <div className="alert alert-error">❌ {error} · Upload a CAS PDF first.</div>
      )}

      {report && !loading && (
        <>
          {/* Summary row */}
          <div className="stat-grid" style={{ marginBottom: 20 }}>
            <div className="stat-card">
              <div className="stat-label">💰 Invested</div>
              <div className="stat-value blue">{fmt(report.totalInvestedAmount)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">📈 Current Value</div>
              <div className="stat-value">{fmt(report.totalCurrentValue)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">🎯 Unrealised Gain</div>
              <div className={`stat-value ${gainColor(report.totalUnrealisedGain)}`}>
                {fmt(report.totalUnrealisedGain)}
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-label">📊 Holdings</div>
              <div className="stat-value">{report.holdings?.length ?? 0}</div>
              <div className="stat-sub">funds</div>
            </div>
          </div>

          {/* Allocation + Holdings */}
          <div className="allocation-grid">
            <div className="card">
              <div className="section-title" style={{ marginBottom: 16 }}>🗂 Allocation</div>
              <PieChart data={alloc} />
            </div>

            <div className="card">
              <div className="section-title" style={{ marginBottom: 16 }}>⚡ Top Holdings</div>
              {(report.holdings ?? []).slice(0, 6).map((h, i) => (
                <div key={i} style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  padding: '8px 0', borderBottom: '1px solid var(--border)'
                }}>
                  <div>
                    <div style={{ fontSize: 13, fontWeight: 500, marginBottom: 2 }}>{h.schemeName}</div>
                    <div style={{ fontSize: 11, color: 'var(--muted)' }}>{h.amcName}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: 13, fontWeight: 600 }}>{fmt(h.currentValue)}</div>
                    <div className={gainColor(h.unrealisedGain)} style={{ fontSize: 11 }}>
                      {fmt(h.unrealisedGain)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Full Holdings Table */}
          <div className="card" style={{ padding: 0 }}>
            <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)' }}>
              <span className="section-title">All Holdings</span>
            </div>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Scheme</th>
                    <th>Category</th>
                    <th>Risk</th>
                    <th>Units</th>
                    <th>Invested</th>
                    <th>Current</th>
                    <th>Gain/Loss</th>
                    <th>XIRR</th>
                  </tr>
                </thead>
                <tbody>
                  {(report.holdings ?? []).map((h, i) => (
                    <tr key={i}>
                      <td>
                        <div style={{ fontWeight: 500, fontSize: 13 }}>{h.schemeName}</div>
                        <div style={{ fontSize: 11, color: 'var(--muted)' }}>
                          {h.planType && <Badge text={h.planType} type={h.planType === 'DIRECT' ? 'green' : 'yellow'} />}
                        </div>
                      </td>
                      <td>
                        {h.broadCategory
                          ? <Badge text={h.broadCategory} type={categoryBadgeType(h.broadCategory)} />
                          : '—'}
                      </td>
                      <td><RiskPips level={h.riskLevel ?? 0} /></td>
                      <td style={{ fontFamily: 'monospace', fontSize: 12 }}>
                        {parseFloat(h.units ?? 0).toFixed(3)}
                      </td>
                      <td>{fmt(h.investedAmount)}</td>
                      <td style={{ fontWeight: 600 }}>{fmt(h.currentValue)}</td>
                      <td className={gainColor(h.unrealisedGain)}>
                        {fmt(h.unrealisedGain)}
                      </td>
                      <td className={gainColor(h.xirr)}>
                        {h.xirr ? pct(h.xirr) : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
