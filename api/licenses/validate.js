const { decodeLicense } = require('./_lib');

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ valid: false, error: 'Méthode non autorisée.' });
  try {
    const body = req.body || {};
    const key = String(body.key || '').trim();
    const license = decodeLicense(key);
    if (!license) return res.status(401).json({ valid: false, error: 'Clé SIRA invalide ou non reconnue.' });
    if (license.expired) {
      return res.status(403).json({
        valid: false,
        expired: true,
        error: `Licence expirée le ${new Date(license.expiresAt).toLocaleDateString('fr-FR')}. Veuillez renouveler votre licence.`
      });
    }
    return res.status(200).json({
      valid: true,
      license: {
        key,
        merchantName: license.merchantName,
        shopName: license.shopName,
        maxUsers: license.maxUsers,
        usedCount: 1,
        durationDays: license.durationDays,
        expiresAt: license.expiresAt,
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
  } catch (error) {
    return res.status(503).json({ valid: false, error: error instanceof Error ? error.message : 'Service de licence indisponible.' });
  }
};
