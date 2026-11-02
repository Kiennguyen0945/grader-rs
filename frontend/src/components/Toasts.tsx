import React, { createContext, useCallback, useContext, useRef, useState } from 'react';
import { Check, Terminal } from './SharedUI';

type ToastKind = 'ok' | 'err' | 'info';

interface ToastItem {
  id: number;
  message: string;
  kind: ToastKind;
  leaving?: boolean;
}

interface ToastCtx {
  push: (message: string, kind?: ToastKind) => void;
}

const Ctx = createContext<ToastCtx>({ push: () => {} });

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const nextId = useRef(1);

  const remove = useCallback((id: number) => {
    setItems((list) => list.map((t) => (t.id === id ? { ...t, leaving: true } : t)));
    window.setTimeout(() => setItems((list) => list.filter((t) => t.id !== id)), 250);
  }, []);

  const push = useCallback(
    (message: string, kind: ToastKind = 'info') => {
      const id = nextId.current++;
      setItems((list) => [...list, { id, message, kind }]);
      window.setTimeout(() => remove(id), 4200);
    },
    [remove],
  );

  return (
    <Ctx.Provider value={{ push }}>
      {children}
      <div id="toasts">
        {items.map((t) => (
          <div
            key={t.id}
            className={`toast ${t.kind === 'ok' ? 't-ok' : t.kind === 'err' ? 't-err' : ''} ${
              t.leaving ? 'out' : ''
            }`}
          >
            <span className="tic">{t.kind === 'ok' ? <Check /> : <Terminal />}</span>
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </Ctx.Provider>
  );
}

export const useToast = () => useContext(Ctx);
