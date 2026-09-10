const { isAdmin } = require('./_auth');

module.exports = async function handler(req, res) {
  if (req.method !== 'GET') return res.status(405).json({ ok: false, error: 'Méthode non autorisée.' });
  return res.status(200).json({ ok: true, authenticated: isAdmin(req) });
};
