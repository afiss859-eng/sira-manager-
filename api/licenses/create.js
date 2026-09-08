const { createLicense } = require('./_lib');

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ ok: false, error: 'Méthode non autorisée.' });
  const body = req.body || {};
  const licenseKey = createLicense(body);
  const stockModel = ['BOUTIQUE', 'NATIONAL', 'INTERNATIONAL'].includes(String(body.stockModel || '').toUpperCase()) ? String(body.stockModel).toUpperCase() : 'BOUTIQUE';
  return res.status(201).json({
    ok: true,
    licenseKey,
    license: {
      key: licenseKey,
      stockModel,
      maxUsers: Math.max(1, Math.min(100000, Number(body.maxUsers) || 1)),
      usedCount: 0,
      profilePhotoUrl: body.profilePhotoUrl || '',
      logoUrl: body.logoUrl || '',
      bgUrl: body.bgUrl || '',
      themeColor: body.themeColor || '#005AC1',
      keypadLayout: body.keypadLayout || 'GRID_4',
      appName: body.appName || 'SIRA Business',
      merchantName: body.merchantName || 'Commerce SIRA',
      shopName: body.shopName || 'SIRA Business'
    }
  });
};
