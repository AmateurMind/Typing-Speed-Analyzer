/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#0f172a',
        mist: '#1e293b',
        paper: '#e2e8f0',
        ok: '#ffffff',
        current: '#facc15',
        bad: '#ef4444',
      },
    },
  },
  plugins: [],
}

