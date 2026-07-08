import React, { createContext, useContext, useEffect, useState } from 'react';

export type Theme = 'light' | 'dark' | 'midnight' | 'forest' | 'sunset';

interface ThemeConfig {
  label: string;
  preview: string; // hex for the swatch
  vars: Record<string, string>;
}

export const THEMES: Record<Theme, ThemeConfig> = {
  light: {
    label: 'Light',
    preview: '#F8FAFC',
    vars: {
      '--bg-app':       '#F8FAFC',
      '--bg-card':      '#FFFFFF',
      '--bg-sidebar':   '#0B1422',
      '--bg-header':    '#FFFFFF',
      '--border':       '#E2E8F0',
      '--text-primary': '#0F172A',
      '--text-secondary': '#64748B',
      '--text-muted':   '#94A3B8',
      '--accent':       '#2563EB',
      '--accent-hover': '#1D4ED8',
      '--success':      '#16A34A',
      '--warning':      '#D97706',
      '--danger':       '#DC2626',
      '--shadow-card':  '0 1px 3px rgba(0,0,0,0.08), 0 4px 16px rgba(0,0,0,0.04)',
    },
  },
  dark: {
    label: 'Dark',
    preview: '#1E293B',
    vars: {
      '--bg-app':       '#0F172A',
      '--bg-card':      '#1E293B',
      '--bg-sidebar':   '#020817',
      '--bg-header':    '#1E293B',
      '--border':       '#334155',
      '--text-primary': '#F1F5F9',
      '--text-secondary': '#94A3B8',
      '--text-muted':   '#64748B',
      '--accent':       '#3B82F6',
      '--accent-hover': '#2563EB',
      '--success':      '#22C55E',
      '--warning':      '#F59E0B',
      '--danger':       '#EF4444',
      '--shadow-card':  '0 1px 3px rgba(0,0,0,0.3), 0 4px 16px rgba(0,0,0,0.2)',
    },
  },
  midnight: {
    label: 'Midnight',
    preview: '#0D0D1A',
    vars: {
      '--bg-app':       '#0D0D1A',
      '--bg-card':      '#13131F',
      '--bg-sidebar':   '#08080F',
      '--bg-header':    '#13131F',
      '--border':       '#1F1F35',
      '--text-primary': '#E2E8F0',
      '--text-secondary': '#8892A4',
      '--text-muted':   '#4B5563',
      '--accent':       '#818CF8',
      '--accent-hover': '#6366F1',
      '--success':      '#34D399',
      '--warning':      '#FBBF24',
      '--danger':       '#F87171',
      '--shadow-card':  '0 1px 3px rgba(0,0,0,0.5), 0 4px 16px rgba(0,0,0,0.4)',
    },
  },
  forest: {
    label: 'Forest',
    preview: '#0F2318',
    vars: {
      '--bg-app':       '#F0FDF4',
      '--bg-card':      '#FFFFFF',
      '--bg-sidebar':   '#0F2318',
      '--bg-header':    '#FFFFFF',
      '--border':       '#BBF7D0',
      '--text-primary': '#052E16',
      '--text-secondary': '#166534',
      '--text-muted':   '#4ADE80',
      '--accent':       '#16A34A',
      '--accent-hover': '#15803D',
      '--success':      '#22C55E',
      '--warning':      '#D97706',
      '--danger':       '#DC2626',
      '--shadow-card':  '0 1px 3px rgba(0,0,0,0.06), 0 4px 16px rgba(0,0,0,0.04)',
    },
  },
  sunset: {
    label: 'Sunset',
    preview: '#1A0A00',
    vars: {
      '--bg-app':       '#FFF7ED',
      '--bg-card':      '#FFFFFF',
      '--bg-sidebar':   '#1A0A00',
      '--bg-header':    '#FFFFFF',
      '--border':       '#FED7AA',
      '--text-primary': '#1C1917',
      '--text-secondary': '#9A3412',
      '--text-muted':   '#C2410C',
      '--accent':       '#EA580C',
      '--accent-hover': '#C2410C',
      '--success':      '#16A34A',
      '--warning':      '#D97706',
      '--danger':       '#DC2626',
      '--shadow-card':  '0 1px 3px rgba(0,0,0,0.06), 0 4px 16px rgba(0,0,0,0.04)',
    },
  },
};

interface ThemeContextValue {
  theme: Theme;
  setTheme: (t: Theme) => void;
  config: ThemeConfig;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setThemeState] = useState<Theme>(() => {
    return (localStorage.getItem('turnout-theme') as Theme) ?? 'light';
  });

  const config = THEMES[theme];

  // Apply CSS variables to :root whenever theme changes
  useEffect(() => {
    const root = document.documentElement;
    Object.entries(config.vars).forEach(([key, value]) => {
      root.style.setProperty(key, value);
    });
    localStorage.setItem('turnout-theme', theme);
  }, [theme, config]);

  const setTheme = (t: Theme) => setThemeState(t);

  return (
    <ThemeContext.Provider value={{ theme, setTheme, config }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = (): ThemeContextValue => {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used inside <ThemeProvider>');
  return ctx;
};
