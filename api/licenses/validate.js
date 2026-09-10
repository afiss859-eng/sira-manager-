const { decodeLicense } = require('./_lib');
const { allowValidation } = require('./_security');

function safeKey(value) {
  return String(value || '').trim().slice(0, 160);
}

function securityHeaders(res) {
  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
}

module.exports = async function handler(req, res) {
  securityHeaders(res);
  if (req.method !== 'POST') return res.status(405).json({ valid: false, error: 'Méthode non autorisée.' });
  if (!allowValidation(req)) {
    res.setHeader('Retry-After', '900');
    return res.status(429).json({ valid: false, error: 'Trop de validations. Réessayez plus tard.' });
  }
  try {
    const key = safeKey(req.body?.key);
    if (!key || key.length < 20) return res.status(400).json({ valid: false, error: 'Clé de licence invalide.' });
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
  } catch (_) {
    return res.status(503).json({ valid: false, error: 'Service de licence indisponible.' });
  }
};
