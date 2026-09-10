const crypto = require('node:crypto');

const COOKIE = 'sira_admin_session';
const TTL_SECONDS = 8 * 60 * 60;

function secret() {
  const value = String(process.env.SIRA_ADMIN_SECRET || process.env.SIRA_LICENSE_SECRET || '').trim();
  if (value.length < 32) throw new Error('SIRA_ADMIN_SECRET ou SIRA_LICENSE_SECRET doit contenir au moins 32 caractères.');
  return value;
}

function b64(value) { return Buffer.from(value, 'utf8').toString('base64url'); }
function sign(payload) { return crypto.createHmac('sha256', secret()).update(payload).digest('base64url'); }

function issueSession() {
  const payload = b64(JSON.stringify({ sub: 'sira-admin', exp: Math.floor(Date.now() / 1000) + TTL_SECONDS }));
  return `${payload}.${sign(payload)}`;
}

function readCookies(header = '') {
  return String(header).split(';').reduce((out, item) => {
    const i = item.indexOf('=');
    if (i > 0) out[item.slice(0, i).trim()] = decodeURIComponent(item.slice(i + 1).trim());
    return out;
  }, {});
}

function verifySession(token) {
  try {
    const [payload, signature] = String(token || '').split('.');
    if (!payload || !signature) return false;
    const expected = sign(payload);
    const a = Buffer.from(signature); const b = Buffer.from(expected);
    if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) return false;
    const data = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
    return data?.sub === 'sira-admin' && Number(data.exp) > Math.floor(Date.now() / 1000);
  } catch (_) { return false; }
}

function isAdmin(req) { return verifySession(readCookies(req.headers?.cookie)[COOKIE]); }

function requireAdmin(req, res) {
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

module.exports = { issueSession, isAdmin, requireAdmin, sessionCookie, clearCookie, COOKIE };
