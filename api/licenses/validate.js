const { decodeLicense } = require('./_lib');

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ valid: false, error: 'Méthode non autorisée.' });
  const body = req.body || {};
  const key = String(body.key || '').trim();
  const license = decodeLicense(key);
  if (!license) return res.status(401).json({ valid: false, error: 'Clé SIRA invalide ou non reconnue.' });
  return res.status(200).json({
    valid: true,
    license: {
      key,
      merchantName: license.merchantName,
      shopName: license.shopName,
      maxUsers: license.maxUsers,
      usedCount: 1,
      stockModel: license.stockModel,
      features: license.features,
      themeColor: license.themeColor,
      appName: license.appName,
      profilePhotoUrl: license.profilePhotoUrl,
      logoUrl: license.logoUrl,
      bgUrl: license.bgUrl,
      cguText: license.cguText,
      privacyText: license.privacyText,
      keypadLayout: license.keypadLayout,
      country: license.country,
      currency: license.currency
    },
    commands: []
  });
};
