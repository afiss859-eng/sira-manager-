const { createLicense, decodeLicense } = require('./_lib');
const { requireAdmin } = require('../admin/_auth');

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ ok: false, error: 'Méthode non autorisée.' });
  if (!requireAdmin(req, res)) return;
  try {
    const body = req.body || {};
    const licenseKey = createLicense(body);
    const license = decodeLicense(licenseKey);
    if (!license) return res.status(500).json({ ok: false, error: 'Impossible de vérifier la licence générée.' });
    return res.status(201).json({
      ok: true,
      licenseKey,
      license: {
        key: licenseKey,
        stockModel: license.stockModel,
        maxUsers: license.maxUsers,
        usedCount: 0,
        durationDays: license.durationDays,
        expiresAt: license.expiresAt,
        profilePhotoUrl: license.profilePhotoUrl,
        logoUrl: license.logoUrl,
        bgUrl: license.bgUrl,
        themeColor: license.themeColor,
        keypadLayout: license.keypadLayout,
        appName: license.appName,
        merchantName: license.merchantName,
        shopName: license.shopName,
        country: license.country,
        currency: license.currency
      }
    });
  } catch (error) {
    return res.status(503).json({ ok: false, error: error instanceof Error ? error.message : 'Service de licence indisponible.' });
  }
};
