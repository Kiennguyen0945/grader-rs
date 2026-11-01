import React, { useState, useEffect } from 'react';
import { apiRequest, getToken, setToken } from './api';

export function App() {
  const [token, setAuthToken] = useState<string | null>(getToken());
  const [username, setUsername] = useState('minh.tran');
  const [password, setPassword] = useState('demo1234');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [courses, setCourses] = useState<any[]>([]);

  useEffect(() => {
    const handleUnauthorized = () => {
      setAuthToken(null);
    };
    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  useEffect(() => {
    if (token) {
      loadCourses();
    }
  }, [token]);

  async function loadCourses() {
    try {
      const data = await apiRequest('/courses');
      setCourses(data);
    } catch (err: any) {
      setError(err.message);
    }
  }

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await apiRequest<{ accessToken: string }>('/auth/login', {
        method: 'POST',
        body: { username, password },
      });
      setToken(res.accessToken);
      setAuthToken(res.accessToken);
    } catch (err: any) {
      setError(err.message || 'Login failed.');
    } finally {
      setLoading(false);
    }
  }

  function handleLogout() {
    setToken(null);
    setAuthToken(null);
    setCourses([]);
  }

  if (!token) {
    return (
      <div id="view-login" style={{ display: 'grid', gridTemplateColumns: '1.1fr 1fr', minHeight: '100vh' }}>
        <div className="login-stage">
          <div className="login-brand">
            <span className="kicker">Lamplight · Grader RS</span>
            <h1>Real-time distributed<br /><em>programming judge</em></h1>
            <p className="mut">Connected directly to Spring Boot 3 &amp; RabbitMQ worker engine.</p>
          </div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', padding: '60px', background: 'var(--panel)' }}>
          <form onSubmit={handleLogin} style={{ maxWidth: '400px', width: '100%', margin: '0 auto' }}>
            <h2 className="pagetitle" style={{ marginBottom: '8px' }}>Sign in</h2>
            <p className="mut small" style={{ marginBottom: '24px' }}>Enter your student or lecturer credentials.</p>
            {error && (
              <div className="banner b-err" style={{ marginBottom: '16px' }}>
                <p>{error}</p>
              </div>
            )}
            <div style={{ marginBottom: '16px' }}>
              <label className="mono small dim" style={{ display: 'block', marginBottom: '6px' }}>USERNAME</label>
              <input
                className="input"
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--line)', background: 'var(--bg-well)', color: 'var(--ink)' }}
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. minh.tran"
                required
              />
            </div>
            <div style={{ marginBottom: '24px' }}>
              <label className="mono small dim" style={{ display: 'block', marginBottom: '6px' }}>PASSWORD</label>
              <input
                type="password"
                className="input"
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--line)', background: 'var(--bg-well)', color: 'var(--ink)' }}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Password"
                required
              />
            </div>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
              style={{ width: '100%', padding: '12px', justifyContent: 'center' }}
            >
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div id="view-app" className="on" style={{ minHeight: '100vh', display: 'grid', gridTemplateColumns: '260px 1fr' }}>
      <aside style={{ background: 'var(--panel)', borderRight: '1px solid var(--line-soft)', padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div className="lampbadge">💡</div>
          <div>
            <div className="bt" style={{ fontWeight: 700 }}>Lamplight</div>
            <div className="bs dim small">Grader RS</div>
          </div>
        </div>
        <div className="navsec mono small dim">NAVIGATION</div>
        <button className="navitem on" style={{ width: '100%', textAlign: 'left', padding: '10px', borderRadius: '8px', background: 'var(--panel-2)', color: 'var(--ink)' }}>
          📚 Courses &amp; Workspace
        </button>
        <div style={{ marginTop: 'auto' }}>
          <button className="btn btn-sec btn-sm" onClick={handleLogout} style={{ width: '100%' }}>
            Sign out
          </button>
        </div>
      </aside>

      <main style={{ padding: '36px', overflowY: 'auto' }}>
        <h1 className="pagetitle" style={{ marginBottom: '8px' }}>Enrolled Courses</h1>
        <p className="mut" style={{ marginBottom: '24px' }}>
          Fetched live from Spring Boot backend through Nginx reverse proxy.
        </p>

        <div className="coursegrid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '18px' }}>
          {courses.length === 0 ? (
            <div className="card pad" style={{ gridColumn: '1 / -1' }}>
              <p className="dim">No enrolled courses returned from backend.</p>
            </div>
          ) : (
            courses.map((course: any) => (
              <div key={course.id} className="coursecard card pad" style={{ border: '1px solid var(--line-soft)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                  <span className="chip" style={{ padding: '3px 8px', borderRadius: '4px', background: 'var(--panel-2)', fontFamily: 'var(--mono)' }}>
                    {course.code}
                  </span>
                </div>
                <h3>{course.courseName}</h3>
                <p className="mut small" style={{ margin: '8px 0 16px' }}>{course.description || 'No description provided.'}</p>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span className="dim small mono">Course ID #{course.id}</span>
                  <button className="btn btn-primary btn-sm">Enter Course</button>
                </div>
              </div>
            ))
          )}
        </div>
      </main>
    </div>
  );
}

