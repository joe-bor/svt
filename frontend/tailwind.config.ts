import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        accent: {
          teal:  '#a8edea',
          pink:  '#fed6e3',
          blue:  '#b3e5fc',
          amber: '#ffe082',
          red:   '#ff6b6b',
        },
      },
      fontFamily: {
        display: ['"Instrument Serif"', 'Georgia', 'serif'],
        sans: ['"Space Grotesk"', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'sans-serif'],
      },
      transitionTimingFunction: {
        'out-quart': 'cubic-bezier(0.25, 1, 0.5, 1)',
        'out-quint': 'cubic-bezier(0.22, 1, 0.36, 1)',
        'out-expo':  'cubic-bezier(0.16, 1, 0.3, 1)',
      },
      keyframes: {
        'fade-in':      { '0%': { opacity: '0' },                                  '100%': { opacity: '1' } },
        'fade-in-up':   { '0%': { opacity: '0', transform: 'translateY(8px)' },    '100%': { opacity: '1', transform: 'translateY(0)' } },
        'fade-in-down': { '0%': { opacity: '0', transform: 'translateY(-12px)' },  '100%': { opacity: '1', transform: 'translateY(0)' } },
        'scale-in':     { '0%': { opacity: '0', transform: 'scale(0.85)' },        '100%': { opacity: '1', transform: 'scale(1)' } },
        'check-in':     { '0%': { opacity: '0', transform: 'scale(0.5)' },         '60%': { opacity: '1', transform: 'scale(1.08)' }, '100%': { opacity: '1', transform: 'scale(1)' } },
        'pulse-flash':  { '0%,100%': { boxShadow: '0 0 0 0 rgba(168,237,234,0)' }, '40%': { boxShadow: '0 0 0 3px rgba(168,237,234,0.35)' } },
      },
      animation: {
        'fade-in':      'fade-in 260ms cubic-bezier(0.25, 1, 0.5, 1) both',
        'fade-in-up':   'fade-in-up 320ms cubic-bezier(0.22, 1, 0.36, 1) both',
        'fade-in-down': 'fade-in-down 260ms cubic-bezier(0.22, 1, 0.36, 1) both',
        'scale-in':     'scale-in 420ms cubic-bezier(0.16, 1, 0.3, 1) both',
        'check-in':     'check-in 280ms cubic-bezier(0.25, 1, 0.5, 1) both',
        'pulse-flash':  'pulse-flash 600ms cubic-bezier(0.25, 1, 0.5, 1) 1',
      },
    },
  },
  plugins: [],
} satisfies Config;
