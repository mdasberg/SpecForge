import { createContext } from 'react';

export type Theme = 'dark' | 'light';

export const STORAGE_KEY = 'specforge.theme';

export interface ThemeContextValue {
  theme: Theme;
  toggle: () => void;
}

/** Split from ThemeProvider so that file exports only a component and keeps fast refresh working. */
export const ThemeContext = createContext<ThemeContextValue | null>(null);
