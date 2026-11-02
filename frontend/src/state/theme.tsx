import React, { createContext, useContext, useEffect, useState } from 'react';
import { Moon, Sun } from '../components/SharedUI';

export type Theme = 'dark' | 'light';

interface ThemeCtx {
  theme: Theme;
  toggle: () => void;
}

const Ctx = createContext<ThemeCtx>({ theme: 'dark', toggle: () => {} });

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = useState<Theme>(() => {
    try {
      const stored = localStorage.getItem('lamplight-theme');
      return stored === 'light' ? 'light' : 'dark';
    } catch {
      return 'dark';
    }
  });

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try {
      localStorage.setItem('lamplight-theme', theme);
    } catch {}
  }, [theme]);

  return (
    <Ctx.Provider value={{ theme, toggle: () => setTheme((t) => (t === 'dark' ? 'light' : 'dark')) }}>
      {children}
    </Ctx.Provider>
  );
}

export const useTheme = () => useContext(Ctx);

export function ThemeToggle({ className = 'iconbtn', title = 'Toggle light / dark' }: { className?: string; title?: string }) {
  const { toggle } = useTheme();
  return (
    <button type="button" className={className} title={title} onClick={toggle}>
      <Sun className="theme-sun" />
      <Moon className="theme-moon" />
    </button>
  );
}
