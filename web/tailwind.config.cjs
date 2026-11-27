/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        gp: {
          primary: '#0F172A',
          secondary: '#0284C7',
          accent: '#10B981',
          warning: '#F59E0B',
          danger: '#EF4444',
          surface: '#FFFFFF',
          bg: '#F1F5F9',
          border: '#E2E8F0',
          muted: '#64748B'
        }
      }
    }
  },
  plugins: []
};
