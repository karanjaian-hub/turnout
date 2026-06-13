/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: {
          50:  '#EFF6FF',
          500: '#2563EB',
          600: '#1D4ED8',
          900: '#1E3A5F',
        },
        navy:    '#1E3A5F',
        success: '#16A34A',
        warning: '#D97706',
        danger:  '#DC2626',
      },
      fontFamily: {
        sans: ['Inter', 'DM Sans', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      borderRadius: {
        card:  '12px',
        input: '8px',
      },
      boxShadow: {
        card:  '0 1px 3px rgba(0,0,0,0.08), 0 8px 24px rgba(0,0,0,0.04)',
        hover: '0 4px 12px rgba(0,0,0,0.12)',
      },
      keyframes: {
        fadeInScale: {
          '0%':   { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
      },
      animation: {
        'fadeInScale': 'fadeInScale 150ms ease',
      },
    },
  },
  plugins: [],
};
