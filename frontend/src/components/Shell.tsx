import React, { useEffect, useRef, useState } from 'react';
import { Avatar, Bell, Book, displayName, Lamp, ListIcon, SignOut } from './SharedUI';
import { ThemeToggle } from '../state/theme';
import { useAuth } from '../state/auth';
import { useCrumb } from '../state/crumb';
import { data } from '../data';
import type { NotificationItem } from '../types';

const GridIcon = ({ size = 17 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
    <rect x="3" y="3" width="8" height="8" rx="2" />
    <rect x="13" y="3" width="8" height="5" rx="2" />
    <rect x="13" y="10" width="8" height="11" rx="2" />
    <rect x="3" y="13" width="8" height="8" rx="2" />
  </svg>
);

function Ambient() {
  return (
    <div className="ambient" aria-hidden="true">
      <div className="glow-lamp" />
      <div className="glow-plum" />
      <span className="ember e1" />
      <span className="ember e2" />
      <div className="grain" />
    </div>
  );
}

const TONE_COLOR: Record<NotificationItem['tone'], string> = {
  amber: 'var(--amber)',
  ok: 'var(--ok)',
  violet: 'var(--violet)',
};

export type ShellSection = 'dashboard' | 'courses' | 'history' | 'workspace';

export function Shell({ active, children }: { active: ShellSection; children: React.ReactNode }) {
  const { session, logout } = useAuth();
  const { crumb } = useCrumb();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [bellOpen, setBellOpen] = useState(false);
  const bellRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    let cancelled = false;
    data
      .notifications()
      .then((items) => {
        if (!cancelled) setNotifications(items);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!bellOpen) return;
    const onDown = (e: MouseEvent) => {
      if (bellRef.current && !bellRef.current.contains(e.target as Node)) setBellOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setBellOpen(false);
    };
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [bellOpen]);

  const username = session?.username ?? 'student';
  const name = displayName(username);
  const role = (session?.roles[0] ?? 'ROLE_STUDENT').replace('ROLE_', '');

  const navItem = (target: ShellSection, label: string, icon: React.ReactNode, href: string) => (
    <a className={active === target ? 'navitem on' : 'navitem'} href={`#${href}`} style={{ textDecoration: 'none' }}>
      {icon}
      {label}
    </a>
  );

  return (
    <>
      <Ambient />
      <div id="view-app" className="on">
        <aside>
          <div className="brandrow">
            <div className="lampbadge">
              <Lamp size={19} />
            </div>
            <div>
              <div className="bt">Lamplight</div>
              <div className="bs">Grader</div>
            </div>
          </div>
          <nav id="sidenav">
            <div className="navsec">student</div>
            {navItem('dashboard', 'Dashboard', <GridIcon />, '/dashboard')}
            {navItem('courses', 'My courses', <Book />, '/courses')}
            {navItem('history', 'Submissions', <ListIcon />, '/history')}
          </nav>
          <div className="side-foot">
            <div className="userchip" id="userchip">
              <Avatar name={name} size={32} />
              <div style={{ minWidth: 0 }}>
                <div className="un">{name}</div>
                <div className="um">
                  {role} · {username}
                </div>
              </div>
            </div>
          </div>
        </aside>

        <header className="topbar">
          <div id="crumb">
            {crumb.parents.map((p) => (
              <React.Fragment key={p}>
                {p} <span style={{ opacity: 0.5 }}>/</span>{' '}
              </React.Fragment>
            ))}
            <b>{crumb.current}</b>
          </div>
          <div className="top-right">
            <ThemeToggle />
            <div className="bellwrap" ref={bellRef}>
              <button
                type="button"
                className="iconbtn bellbtn"
                aria-label="Notifications"
                onClick={() => setBellOpen((v) => !v)}
              >
                <Bell />
                {notifications.length > 0 && <span className="ndot" />}
              </button>
              <div className={bellOpen ? 'notifpop open' : 'notifpop'} id="notifpop">
                <div
                  style={{
                    padding: '8px 10px 4px',
                    fontFamily: 'var(--mono)',
                    fontSize: '10.5px',
                    letterSpacing: '.14em',
                    color: 'var(--dim)',
                  }}
                >
                  NOTIFICATIONS
                </div>
                {notifications.length === 0 ? (
                  <div className="notif">
                    <span className="nd" style={{ background: 'var(--dim)' }} />
                    <div>
                      <p>You are all caught up.</p>
                      <div className="nt">just now</div>
                    </div>
                  </div>
                ) : (
                  notifications.map((n) => (
                    <div className="notif" key={n.text}>
                      <span className="nd" style={{ background: TONE_COLOR[n.tone] }} />
                      <div>
                        <p>{n.text}</p>
                        <div className="nt">{n.when}</div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
            <button type="button" className="iconbtn" title="Sign out" onClick={logout}>
              <SignOut />
            </button>
          </div>
        </header>

        <main id="main">{children}</main>
      </div>

      <div className="overlay" id="overlay" />
      <div className="modal" id="modal" role="dialog" aria-modal="true" />
    </>
  );
}
