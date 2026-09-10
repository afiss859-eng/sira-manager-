const { clearCookie } = require('./_auth');

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ ok: false, error: 'Méthode non autorisée.' });
  res.setHeader('Set-Cookie', clearCookie());
  return res.status(200).json({ ok: true, authenticated: false });
};
