const crypto = require('node:crypto');

const COOKIE = 'sira_admin_session';
const TTL_SECONDS = 8 * 60 * 60;
const attempts = new Map();
const WINDOW_MS = 15 * 60 * 1000;
const MAX_FAILS = 8;

function secret() {
  const value = String(process.env.SIRA_ADMIN_SECRET || process.env.SIRA_LICENSE_SECRET || '').trim();
  if (value.length < 32) throw new Error('SIRA_ADMIN_SECRET ou SIRA_LICENSE_SECRET doit contenir au moins 32 caractères.');
  return value;
}

function b64(value) { return Buffer.from(value, 'utf8').toString('base64url'); }
function sign(payload) { return crypto.createHmac('sha256', secret()).update(payload).digest('base64url'); }

function clientId(req) {
  const forwarded = String(req.headers?.['x-forwarded-for'] || '').split(',')[0].trim();
  return forwarded || String(req.headers?.['x-real-ip'] || 'unknown');
}

function rateLimitKey(req) { return `admin:${clientId(req)}`; }
function allowAttempt(req) {
  const now = Date.now();
  const key = rateLimitKey(req);
  const item = attempts.get(key);
  if (!item || now - item.startedAt > WINDOW_MS) {
    attempts.set(key, { startedAt: now, fails: 0 });
    return true;
  }
  return item.fails < MAX_FAILS;
}

function recordFailure(req) {
  const now = Date.now();
  const key = rateLimitKey(req);
  const item = attempts.get(key);
  if (!item || now - item.startedAt > WINDOW_MS) attempts.set(key, { startedAt: now, fails: 1 });
  else item.fails += 1;
}

function clearFailures(req) { attempts.delete(rateLimitKey(req)); }

function allowedOrigin(req) {
  const origin = String(req.headers?.origin || '').trim();
  if (!origin) return true;
  const configured = String(process.env.SIRA_ADMIN_ORIGIN || '').trim();
  if (!configured) return false;
  return origin === configured;
}

function issueSession() {
  const payload = b64(JSON.stringify({ sub: 'sira-admin', exp: Math.floor(Date.now() / 1000) + TTL_SECONDS, jti: crypto.randomBytes(12).toString('hex') }));
  return `${payload}.${sign(payload)}`;
}

function readCookies(header = '') {
  return String(header).split(';').reduce((out, item) => {
    const i = item.indexOf('=');
    if (i > 0) {
      try { out[item.slice(0, i).trim()] = decodeURIComponent(item.slice(i + 1).trim()); } catch (_) {}
    }
    return out;
  }, {});
}

function verifySession(token) {
  try {
    const [payload, signature, extra] = String(token || '').split('.');
    if (!payload || !signature || extra) return false;
    const expected = sign(payload);
    const a = Buffer.from(signature); const b = Buffer.from(expected);
    if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) return false;
    const data = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
    return data?.sub === 'sira-admin' && typeof data.jti === 'string' && data.jti.length >= 16 && Number(data.exp) > Math.floor(Date.now() / 1000);
  } catch (_) { return false; }
}

function isAdmin(req) { return allowedOrigin(req) && verifySession(readCookies(req.headers?.cookie)[COOKIE]); }

function requireAdmin(req, res) {
  if (!allowedOrigin(req)) {
    res.status(403).json({ ok: false, authenticated: false, error: 'Origine non autorisée.' });
    return false;
  }
  if (isAdmin(req)) return true;
  res.status(401).json({ ok: false, authenticated: false, error: 'Authentification administrateur requise.' });
  return false;
}

function sessionCookie(token) {
  return `${COOKIE}=${encodeURIComponent(token)}; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=${TTL_SECONDS}`;
}

function clearCookie() {
  return `${COOKIE}=; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=0`;
}

module.exports = { issueSession, isAdmin, requireAdmin, sessionCookie, clearCookie, COOKIE, allowedOrigin, allowAttempt, recordFailure, clearFailures };
