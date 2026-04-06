import { useState, useEffect } from 'react';
import { fetchTaxSummary } from '../api';
import { fmt } from '../utils.jsx';

function TaxRow({ label, value, highlight }) {
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-between', padding: '12px 0',
      borderBottom: '1px solid var(--border)',
      fontWeight: highlight ? 700 : 400,
      fontSize: highlight ? 15 : 13,
    }}>
      <span style={{ color: highlight ? 'var(--text)' : 'var(--muted)' }}>{label}</span>
      <span className={highlight ? 'red' : ''}>{fmt(value)}</span>
    </div>
  );
}

export default function Tax({ userId }) {
  const [tax, setTax]     = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [inputId, setInputId] = useState(userId);

  async function load(uid) {
    setLoading(true); setError('');
    try {
      const data = await fetchTaxSummary(uid);
      if (data.message) { setError(data.message); setTax(null); }
      else setTax(data);
    } catch { setError('Could not fetch tax summary.'); }
    setLoading(false);
  }

  useEffect(() => { load(userId); }, [userId]);

  const LTCG_EXEMPTION = 125000;
  const LTCG_RATE = 12.5;
  const STCG_RATE = 20;

  return (
    <div>
      <p className="page-title">Tax Summary</p>
      <p className="page-sub">FY LTCG &amp; STCG breakdown — SEBI / Budget 2024 rules</p>

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

      {tax && !loading && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, maxWidth: 900 }}>

          {/* LTCG Block */}
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
              <span style={{ fontSize: 22 }}>📅</span>
              <div>
                <div style={{ fontWeight: 700, fontSize: 15 }}>Long-Term Capital Gains</div>
                <div style={{ fontSize: 11, color: 'var(--muted)' }}>Equity held &gt; 1 year · {LTCG_RATE}% tax</div>
              </div>
            </div>

            <TaxRow label="Total LTCG gains" value={tax.totalLtcgGains} />
            <TaxRow label={`Exemption (₹1.25 L limit)`} value={-Math.min(LTCG_EXEMPTION, Math.max(0, parseFloat(tax.totalLtcgGains ?? 0)))} />
            <TaxRow label="Taxable LTCG" value={tax.taxableLtcg} />
            <TaxRow label={`LTCG Tax @ ${LTCG_RATE}%`} value={tax.ltcgTaxPayable} highlight />

            <div className="alert alert-info" style={{ marginTop: 16, marginBottom: 0, fontSize: 12 }}>
              ℹ️ Budget 2024: LTCG exemption is ₹1.25 L/year. Gains above this taxed at {LTCG_RATE}% (no indexation).
            </div>
          </div>

          {/* STCG Block */}
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
              <span style={{ fontSize: 22 }}>⚡</span>
              <div>
                <div style={{ fontWeight: 700, fontSize: 15 }}>Short-Term Capital Gains</div>
                <div style={{ fontSize: 11, color: 'var(--muted)' }}>Equity held ≤ 1 year · {STCG_RATE}% tax</div>
              </div>
            </div>

            <TaxRow label="Total STCG gains" value={tax.totalStcgGains} />
            <TaxRow label="STCG Tax @ 20%" value={tax.stcgTaxPayable} highlight />

            <div className="alert alert-info" style={{ marginTop: 16, marginBottom: 0, fontSize: 12 }}>
              ℹ️ Budget 2024: STCG on equity funds now taxed at {STCG_RATE}% (raised from 15%).
            </div>
          </div>

          {/* Total */}
          <div className="card" style={{ gridColumn: '1 / -1' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 4 }}>Total Estimated Tax Liability</div>
                <div style={{ fontSize: 11, color: 'var(--muted)' }}>
                  LTCG tax + STCG tax (surcharge / cess not included)
                </div>
              </div>
              <div className="red" style={{ fontSize: 32, fontWeight: 800, letterSpacing: -1 }}>
                {fmt(tax.totalTaxPayable)}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
