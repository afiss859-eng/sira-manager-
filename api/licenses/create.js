const { createLicense } = require('./_lib');

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ ok: false, error: 'Méthode non autorisée.' });
  const body = req.body || {};
  const licenseKey = createLicense(body);
  return res.status(201).json({ ok: true, licenseKey, license: { stockModel: String(body.stockModel || 'BOUTIQUE').toUpperCase(), maxUsers: Math.max(1, Math.min(100000, Number(body.maxUsers) || 1)), appName: body.appName || 'SIRA Business', merchantName: body.merchantName || 'Commerce SIRA', shopName: body.shopName || 'SIRA Business' } });
};
