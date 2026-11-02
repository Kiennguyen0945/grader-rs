import React, { useState } from 'react';
import { Eye } from '../components/SharedUI';
import { ThemeToggle } from '../state/theme';
import { useAuth } from '../state/auth';
import { demoLoginMeta } from '../dev/fixtures';
import { data } from '../data';
import { navigate } from '../router';

const HangLamp = () => (
  <svg className="hanglamp" width="340" height="430" viewBox="0 0 340 430" fill="none" aria-hidden="true">
    <line className="wire" x1="170" y1="0" x2="170" y2="140" strokeWidth="2.5" />
    <path className="shade" d="M138 140 L202 140 L216 178 L124 178 Z" strokeWidth="1.5" />
    <circle className="bulb-glow" cx="170" cy="196" r="86" fill="url(#lg)" />
    <circle cx="170" cy="190" r="15" fill="#F2C879" />
    <circle cx="170" cy="190" r="24" fill="rgba(232,180,90,.28)" />
    <path d="M124 182 L60 430 L280 430 L216 182" fill="url(#cone)" opacity=".5" />
    <defs>
      <radialGradient id="lg">
        <stop offset="0" stopColor="rgba(232,180,90,.5)" />
        <stop offset="1" stopColor="rgba(232,180,90,0)" />
      </radialGradient>
      <linearGradient id="cone" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stopColor="rgba(232,180,90,.22)" />
        <stop offset="1" stopColor="rgba(232,180,90,0)" />
      </linearGradient>
    </defs>
  </svg>
);

export function LoginView() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const demo = data.demo;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(username.trim(), password);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err?.message || 'Sign in failed. Check your username and password.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div id="view-login">
      <div className="login-stage">
        <ThemeToggle className="iconbtn login-themebtn" />
        <HangLamp />
        <div className="codefloat" style={{ top: '16%', left: '8%' }}>
          {`while (queue.hasNext()) {\n  judge.run(test);\n}`}
        </div>
        <div className="codefloat" style={{ top: '34%', right: '10%', animationDelay: '3s' }}>
          {`if (expected.equals(actual))\n  grade += points;`}
        </div>
        <div className="codefloat" style={{ bottom: '34%', left: '14%', animationDelay: '6s' }}>
          {`ASSERT list.size() == 3\n// Lab 3 · linked list`}
        </div>
        <div className="login-brand">
          <span className="kicker">Lamplight Grader · Fall 2026</span>
          <h1>
            Grade code by <em>lamplight.</em>
          </h1>
          <p>
            An online code-grading system for students, lecturers and admins — submissions, judging, test cases and
            grades, tuned for late-night sessions.
          </p>
          {demo && (
            <div className="login-meta">
              {demoLoginMeta.map((m) => (
                <span key={m.text}>
                  <b>{m.strong}</b> {m.text}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="login-panel">
        <form className="login-card" id="login-form" noValidate onSubmit={handleSubmit}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '13px' }}>
            <div className="login-logo">
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                <path d="M9 3h6l1.5 5h-9L9 3Z" />
                <path d="M12 8v3" />
                <circle cx="12" cy="13.5" r="2.4" />
                <path d="M7.5 18.5c1-1.6 8-1.6 9 0" opacity=".6" />
              </svg>
            </div>
            <div>
              <div className="login-word">Lamplight</div>
              <div className="login-wordsub">Grader</div>
            </div>
          </div>
          <h2>Sign in to your course</h2>
          <div className="sub">
            {demo ? 'CS201 · Data Structures · Fall 2026' : 'Use the account issued by your lecturer'}
          </div>
          <div className={error ? 'login-err show' : 'login-err'} id="login-err">
            {error}
          </div>
          <div className="field">
            <label htmlFor="li-user">Username</label>
            <input
              type="text"
              id="li-user"
              placeholder="e.g. minh.tran"
              autoComplete="off"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="li-pass">Password</label>
            <div className="input-wrap">
              <input
                type={showPassword ? 'text' : 'password'}
                id="li-pass"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              <button
                type="button"
                className="eye"
                aria-label="Show password"
                onClick={() => setShowPassword((v) => !v)}
              >
                <Eye />
              </button>
            </div>
          </div>
          <button
            className="btn btn-primary"
            style={{ width: '100%', padding: '11px' }}
            id="login-btn"
            type="submit"
            disabled={loading}
          >
            {loading ? <span className="spin" /> : null}
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
          {demo && (
            <button
              className="btn btn-ghost btn-sm"
              style={{ width: '100%', marginTop: '9px' }}
              type="button"
              onClick={() => {
                setUsername('minh.tran');
                setPassword('demo1234');
              }}
            >
              Use demo student credentials
            </button>
          )}
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '13px' }} className="small">
            <a href="#" onClick={(e) => e.preventDefault()}>
              Forgot password?
            </a>
            <span className="dim">{demo ? 'Demo build · any password works' : 'JWT session · 15 min'}</span>
          </div>
        </form>
      </div>
    </div>
  );
}
