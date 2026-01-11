export default {
  content: ["./index.html", "./src*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        display: ['"Space Grotesk"', '"Inter"', 'system-ui', 'sans-serif'],
      },
      colors: {
        night: '#0b1021',
      },
      boxShadow: {
        glow: '0 0 25px rgba(128, 90, 213, 0.35)',
      },
    },
  },
  plugins: [],
}
