import { useState, useEffect, useCallback } from 'react';
import { fetchSchemes } from '../api';
import { Badge, categoryBadgeType } from '../utils.jsx';

export default function Schemes() {
  const [schemes, setSchemes]   = useState([]);
  const [total, setTotal]       = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage]         = useState(0);
  const [amc, setAmc]           = useState('');
  const [search, setSearch]     = useState('');
  const [loading, setLoading]   = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchSchemes(page, 15, amc);
      setSchemes(data.content ?? []);
      setTotal(data.totalElements ?? 0);
      setTotalPages(data.totalPages ?? 0);
    } catch {
      setSchemes([]);
    }
    setLoading(false);
  }, [page, amc]);

  useEffect(() => { load(); }, [load]);

  const filtered = search
    ? schemes.filter(s => s.schemeName?.toLowerCase().includes(search.toLowerCase()))
    : schemes;

  return (
    <div>
      <p className="page-title">Fund Schemes</p>
      <p className="page-sub">{total.toLocaleString()} schemes in the database</p>

      <div className="search-bar">
        <input
          className="input input-grow"
          placeholder="Filter by name on this page…"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <input
          className="input"
          style={{ width: 200 }}
          placeholder="AMC (e.g. HDFC)"
          value={amc}
          onChange={e => { setAmc(e.target.value); setPage(0); }}
        />
        <button className="btn btn-ghost" onClick={() => { setAmc(''); setSearch(''); setPage(0); }}>
          Clear
        </button>
      </div>

      <div className="card" style={{ padding: 0 }}>
        {loading ? (
          <div className="spinner" />
        ) : filtered.length === 0 ? (
          <div className="empty">No schemes found.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>AMFI Code</th>
                  <th>Scheme Name</th>
                  <th>AMC</th>
                  <th>Plan</th>
                  <th>Option</th>
                  <th>Category</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(s => (
                  <tr key={s.amfiCode}>
                    <td style={{ fontFamily: 'monospace', fontSize: 12, color: 'var(--muted)' }}>
                      {s.amfiCode}
                    </td>
                    <td style={{ maxWidth: 320 }}>
                      <span style={{ fontWeight: 500, fontSize: 13 }}>{s.schemeName}</span>
                    </td>
                    <td style={{ color: 'var(--muted)', fontSize: 12 }}>{s.amc}</td>
                    <td>
                      <Badge text={s.planType ?? '—'} type={s.planType === 'DIRECT' ? 'green' : 'yellow'} />
                    </td>
                    <td>
                      <Badge text={s.optionType ?? '—'} type={s.optionType === 'GROWTH' ? 'blue' : 'gray'} />
                    </td>
                    <td>
                      {s.category?.broadCategory
                        ? <Badge text={s.category.broadCategory} type={categoryBadgeType(s.category.broadCategory)} />
                        : <span style={{ color: 'var(--muted)', fontSize: 12 }}>—</span>
                      }
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {!loading && totalPages > 1 && (
        <div className="pagination">
          <button className="page-btn" onClick={() => setPage(0)} disabled={page === 0}>«</button>
          <button className="page-btn" onClick={() => setPage(p => p - 1)} disabled={page === 0}>‹</button>
          <span style={{ fontSize: 12, color: 'var(--muted)', padding: '0 8px' }}>
            Page {page + 1} / {totalPages}
          </span>
          <button className="page-btn" onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>›</button>
          <button className="page-btn" onClick={() => setPage(totalPages - 1)} disabled={page >= totalPages - 1}>»</button>
        </div>
      )}
    </div>
  );
}
