/**
 * Demo-mode switch.
 *
 * The prototype screens contain a few elements the Spring Boot API does not
 * expose (leaderboard rows, student counts, term/lecturer, submission
 * timestamps, per-test time/memory limits). With `?demo=1` in a dev build the
 * UI renders the exact prototype data so the design can be reviewed 1:1.
 * It can never ship: `import.meta.env.DEV` is false in `npm run build`.
 */
export function isDemo(): boolean {
  if (!import.meta.env.DEV) return false;
  try {
    return new URLSearchParams(window.location.search).has('demo');
  } catch {
    return false;
  }
}
