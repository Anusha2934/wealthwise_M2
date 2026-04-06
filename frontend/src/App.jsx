import { useState } from 'react';
import Dashboard from './pages/Dashboard';
import Schemes   from './pages/Schemes';
import Upload    from './pages/Upload';
import Portfolio from './pages/Portfolio';
import Tax       from './pages/Tax';

const NAV = [
  { id: 'dashboard', icon: '🏠', label: 'Dashboard' },
  { id: 'schemes',   icon: '📊', label: 'Schemes' },
  { id: 'upload',    icon: '📁', label: 'Upload CAS' },
  { id: 'portfolio', icon: '💼', label: 'Portfolio' },
  { id: 'tax',       icon: '🧾', label: 'Tax Summary' },
];

export default function App() {
  const [page, setPage]   = useState('dashboard');
  const [userId, setUserId] = useState(1);

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="logo">
          <div className="logo-icon">₹</div>
          <div>
            <div className="logo-text">WealthWise</div>
            <div className="logo-sub">Portfolio Tracker</div>
          </div>
        </div>

        {NAV.map(n => (
          <button
            key={n.id}
            className={`nav-item${page === n.id ? ' active' : ''}`}
            onClick={() => setPage(n.id)}
          >
            <span className="nav-icon">{n.icon}</span>
            {n.label}
          </button>
        ))}

        {/* User ID switcher at bottom */}
        <div style={{ marginTop: 'auto', paddingTop: 20, borderTop: '1px solid var(--border)' }}>
          <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 6, paddingLeft: 4 }}>
            ACTIVE USER ID
          </div>
          <input
            type="number"
            min="1"
            className="input"
            style={{ width: '100%', fontSize: 18, fontWeight: 700, color: 'var(--accent)', textAlign: 'center' }}
            value={userId}
            onChange={e => setUserId(Number(e.target.value))}
          />
        </div>
      </aside>

      <main className="main">
        {page === 'dashboard' && <Dashboard />}
        {page === 'schemes'   && <Schemes />}
        {page === 'upload'    && <Upload userId={userId} />}
        {page === 'portfolio' && <Portfolio userId={userId} />}
        {page === 'tax'       && <Tax userId={userId} />}
      </main>
    </div>
  );
}
