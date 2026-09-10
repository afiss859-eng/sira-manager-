const attempts = new Map();
const WINDOW_MS = 15 * 60 * 1000;
const MAX_REQUESTS = 30;

function clientId(req) {
  const forwarded = String(req.headers?.['x-forwarded-for'] || '').split(',')[0].trim();
  return forwarded || String(req.headers?.['x-real-ip'] || 'unknown');
}

function allowValidation(req) {
  const now = Date.now();
  const key = clientId(req);
  const current = attempts.get(key);
  if (!current || now - current.startedAt > WINDOW_MS) {
    attempts.set(key, { startedAt: now, count: 1 });
    return true;
  }
  if (current.count >= MAX_REQUESTS) return false;
  current.count += 1;
  return true;
}

module.exports = { allowValidation };
