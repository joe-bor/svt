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
    },
  },
  plugins: [],
} satisfies Config;
