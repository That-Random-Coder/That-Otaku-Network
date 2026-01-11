# That Otaku Network — Frontend

A focused React app built with Vite for a small anime-focused social network. This repository contains the frontend: a polished single-page app with feed, groups, profiles, post composer, and social interactions like likes/dislikes, comments, and sharing.

Built to be straightforward to run locally and deploy to modern static hosts (Vercel, Netlify, etc.).

---

## 🚀 What this app does

- Feed: recommendation-based feed (all / following), paginated with infinite scroll.
- Groups: create and join groups; group profile with posts for group members.
- Posting: rich post composer with image upload & base64/media support.
- Profile: user profiles, settings, avatar upload, and preferences for recommendations.
- Reactions & interactions: likes (thumbs-up), dislikes, comments, shares, and favorites.
- Robust media loading: converts data URIs and base64 payloads to Blob/object URLs and fetches remote media with Authorization-only headers to avoid large request headers (prevents 431 errors).
- Accessibility and UX polish: keyboard-friendly forms, loading indicators, and consistent visual design with Tailwind and subtle motion.

---

## 🧩 Tech stack

- React (JSX, hooks)
- Vite (dev server and build)
- Tailwind CSS
- Framer Motion for animations
- lucide-react for icons
- Modern fetch usage for API calls and bearer token handling

---

## 🛠 Local development

Prerequisites:
- Node 18+ (recommended)
- npm (or pnpm/yarn if you prefer)

Steps:

1. Clone the repository:

   ```bash
   git clone https://github.com/That-Random-Coder/That-Otaku-Network.git
   cd That-Otaku-Network
   ```

2. Install dependencies:

   ```bash
   npm ci
   ```

3. Create a `.env` file (you can copy `.env.example`) and set the backend API base URL:

   ```text
   VITE_API_BASE_URL=http://localhost:11111/api/
   ```

   - The app uses `import.meta.env.VITE_API_BASE_URL` for all API requests. Adjust it for your staging/prod backend as needed.

4. Start the dev server:

   ```bash
   npm run dev
   ```

5. Open http://localhost:5173 (or the URL Vite prints) and explore.

---

## 📦 Build & preview

- Build:

  ```bash
  npm run build
  ```

- Preview the production build locally:

  ```bash
  npm run preview
  ```

---

## ☁️ Deployment notes

This is a static frontend. Recommended hosts:

- Vercel: connect the repo, set the `VITE_API_BASE_URL` environment variable, build command `npm run build`, and output directory `dist`.
- Netlify: same as above—set env var in the site settings and deploy.
- GitHub Pages: build locally and publish the `dist/` folder (or use an action to build and push to `gh-pages`).

CORS and auth: make sure your backend allows requests from your frontend origin and accepts Authorization header tokens. The frontend sends the token as an `Authorization: Bearer <token>` header.

---

## 🔐 Authentication & API

- Auth token source: `AccessToken` cookie or localStorage. The frontend normalizes and sends it as `Authorization: Bearer <token>`.
- Some image fetches intentionally avoid sending cookies and instead use Authorization-only fetch to prevent `431 Request Header Fields Too Large` errors (large cookies + data URLs can trigger this).
- If you control the backend, prefer returning absolute image URLs and allow `Authorization` for protected media.

---

## 🧪 Tests & quality

- No automated tests are included yet. You can add unit tests (Jest or Vitest + React Testing Library) and end-to-end tests (Playwright or Cypress).
- ESLint & formatting: follow the existing configuration; run linting before committing.

---

## 🧭 Troubleshooting

- Blank page on `Post` view: ensure the backend returns `includeMedia=true` or compatible media payloads.
- 431 errors when loading media: confirm media fetching is done without cookies (the app converts long data: URIs to blobs and uses object URLs) or adjust server CORS/headers.
- Dev server not starting: check Node version and run `npm ci` again.

---

## 🤝 Contributing

Contributions are welcome. A simple flow:

1. Fork the repo
2. Create a branch (`feat/some-feature`)
3. Make changes and commit with clear messages
4. Open a pull request describing the change and why it helps

Please run the dev server to confirm the UI before requesting a review.

---

## ⚖️ License

This project is provided without an attached license in the repo. Add a `LICENSE` file (MIT is a common choice) to make reuse terms explicit.

---

If you'd like, I can add a short `CONTRIBUTING.md`, a basic issue template, and a GitHub Action to automatically build deploy previews. Want me to add those next? 
