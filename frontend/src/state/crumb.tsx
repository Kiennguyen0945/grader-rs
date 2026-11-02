import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

export interface Crumb {
  parents: string[];
  current: string;
}

interface CrumbCtx {
  crumb: Crumb;
  setCrumb: (crumb: Crumb) => void;
}

const Ctx = createContext<CrumbCtx>({
  crumb: { parents: ['Home'], current: 'Dashboard' },
  setCrumb: () => {},
});

export function CrumbProvider({ children }: { children: ReactNode }) {
  const [crumb, setCrumb] = useState<Crumb>({ parents: ['Home'], current: 'Dashboard' });
  return <Ctx.Provider value={{ crumb, setCrumb }}>{children}</Ctx.Provider>;
}

export const useCrumb = () => useContext(Ctx);

/** Views call this once their data is loaded so the topbar crumb matches the prototype. */
export function useSetCrumb(parents: string[], current: string) {
  const { setCrumb } = useCrumb();
  const key = `${parents.join('|')}::${current}`;
  useEffect(() => {
    setCrumb({ parents: parents.slice(), current });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, setCrumb]);
}

