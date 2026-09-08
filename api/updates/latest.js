const REPO = 'afiss859-eng/sira-manager-';

module.exports = async function handler(req, res) {
  if (req.method !== 'GET') return res.status(405).json({ ok: false, error: 'Méthode non autorisée.' });

  const app = String(req.query?.app || 'business').toLowerCase();
  const currentVersion = String(req.query?.version || '0.0.0');
  const assetMatchers = app === 'orange' ? ['orange', 'money'] : ['business'];
  if (!['business', 'orange'].includes(app)) return res.status(400).json({ ok: false, error: 'Application inconnue.' });

  try {
    const response = await fetch(`https://api.github.com/repos/${REPO}/releases/latest`, {
      headers: { Accept: 'application/vnd.github+json', 'User-Agent': 'SIRA-Manager-Update-Service' }
    });
    if (!response.ok) return res.status(404).json({ ok: false, available: false, error: 'Aucune version publiée.' });

    const release = await response.json();
    const apk = (Array.isArray(release.assets) ? release.assets : []).find(asset => {
      const name = String(asset.name || '').toLowerCase();
      return name.endsWith('.apk') && assetMatchers.every(token => name.includes(token));
    });
    if (!apk) return res.status(404).json({ ok: false, available: false, error: `APK ${app} introuvable dans la dernière release.` });

    const latestVersion = String(release.tag_name || '').replace(/^v/i, '') || '0.0.0';
    const normalize = value => value.split(/[._+-]/).slice(0, 3).map(x => Number.parseInt(x, 10) || 0);
    const a = normalize(currentVersion), b = normalize(latestVersion);
    const available = b[0] > a[0] || (b[0] === a[0] && (b[1] > a[1] || (b[1] === a[1] && b[2] > a[2])));

    res.setHeader('Cache-Control', 'public, s-maxage=300, stale-while-revalidate=600');
    return res.status(200).json({
      ok: true, available, application: app, currentVersion, latestVersion,
      versionCode: release.id || 0,
      mandatory: available,
      title: release.name || `SIRA ${app} ${latestVersion}`,
      notes: release.body || '', apkName: apk.name,
      apkUrl: apk.browser_download_url,
      publishedAt: release.published_at || release.created_at || null
    });
  } catch (error) {
    return res.status(503).json({ ok: false, available: false, error: 'Service de mise à jour temporairement indisponible.' });
  }
};
