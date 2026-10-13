/** Completed years between `dob` (yyyy-MM-dd) and today, or null when the date is not usable. */
export function ageFrom(dob: string | null | undefined, today = new Date()): number | null {
  if (!dob || !/^\d{4}-\d{2}-\d{2}$/.test(dob)) return null;
  const [y, m, d] = dob.split('-').map(Number);
  const birth = new Date(y, m - 1, d);
  if (Number.isNaN(birth.getTime()) || birth > today) return null;
  let age = today.getFullYear() - y;
  if (today.getMonth() < m - 1 || (today.getMonth() === m - 1 && today.getDate() < d)) age--;
  return age;
}

/** Human-readable age, e.g. "34 years" or "7 months" for infants. */
export function ageLabel(dob: string | null | undefined, today = new Date()): string {
  const years = ageFrom(dob, today);
  if (years === null) return '';
  if (years >= 1) return `${years} year${years === 1 ? '' : 's'}`;
  const [y, m, d] = dob!.split('-').map(Number);
  let months = (today.getFullYear() - y) * 12 + today.getMonth() - (m - 1);
  if (today.getDate() < d) months--;
  return months <= 0 ? 'Under 1 month' : `${months} month${months === 1 ? '' : 's'}`;
}

export function isoToday(): string {
  const t = new Date();
  return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`;
}

export function initials(name: string): string {
  return name
    .replace(/^Dr\.?\s+/i, '')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('');
}

/** Rounds to paise to avoid floating point drift in on-screen totals. */
export function money(n: number): number {
  return Math.round((n + Number.EPSILON) * 100) / 100;
}

/** Only allow in-app absolute paths as post-login redirects (blocks open redirects such as //evil.com). */
export function safeReturnUrl(url: string | null | undefined, fallback: string): string {
  return url && url.startsWith('/') && !url.startsWith('//') && !url.startsWith('/\\') ? url : fallback;
}
