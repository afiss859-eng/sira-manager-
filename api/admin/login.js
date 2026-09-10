const crypto = require('node:crypto');
const { issueSession, sessionCookie } = require('./_auth');

function expectedSecret() {
  const value = String(process.env.SIRA_ADMIN_SECRET || '').trim();
  if (value.length < 32) throw new Error('SIRA_ADMIN_SECRET non configuré ou trop court.');
  return value;
}

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ ok: false, error: 'Méthode non autorisée.' });
  try {
    const supplied = String(req.body?.secret || '');
    const expected = expectedSecret();
    const a = Buffer.from(supplied); const b = Buffer.from(expected);
    if (!a.length || a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
      return res.status(401).json({ ok: false, authenticated: false, error: 'Identifiants administrateur invalides.' });
    }
    res.setHeader('Set-Cookie', sessionCookie(issueSession()));
    return res.status(200).json({ ok: true, authenticated: true, expiresIn: 8 * 60 * 60 });
  } catch (error) {
    return res.status(503).json({ ok: false, error: error instanceof Error ? error.message : 'Service d’authentification indisponible.' });
  }
};
