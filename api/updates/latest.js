const REPO = 'afiss859-eng/sira-manager-';

function versionParts(value) {
  return String(value || '0.0.0').split(/[._+-]/).slice(0, 3).map(part => Number.parseInt(part, 10) || 0);
}

function isNewer(installed, published) {
  const a = versionParts(installed);
  const b = versionParts(published);
  return b[0] > a[0] || (b[0] === a[0] && (b[1] > a[1] || (b[1] === a[1] && b[2] > a[2])));
}

module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') return res.status(204).end();
  if (req.method !== 'GET') return res.status(405).json({ ok: false, error: 'Méthode non autorisée.' });

  const app = String(req.query?.app || 'business').toLowerCase();
  const currentVersion = String(req.query?.version || '0.0.0');
  if (!['business', 'orange'].includes(app)) return res.status(400).json({ ok: false, error: 'Application inconnue.' });

  const match = name => {
    const lower = String(name || '').toLowerCase();
    if (!lower.endsWith('.apk')) return false;
    return app === 'orange'
      ? lower.includes('orange') && lower.includes('money')
      : lower.includes('business');
  };
  const bothApks = assets => {
    const names = (Array.isArray(assets) ? assets : []).map(x => String(x.name || '').toLowerCase());
    return names.some(x => x.endsWith('.apk') && x.includes('business')) && names.some(x => x.endsWith('.apk') && x.includes('orange') && x.includes('money'));
  };

  try {
    const response = await fetch(`https://api.github.com/repos/${REPO}/releases?per_page=20`, {
      headers: { Accept: 'application/vnd.github+json', 'User-Agent': 'SIRA-Manager-Update-Service' }
    });
    if (!response.ok) throw new Error(`GitHub ${response.status}`);

    const releases = await response.json();
    const release = (Array.isArray(releases) ? releases : []).find(item => !item.draft && bothApks(item.assets));
    if (!release) {
      res.setHeader('Cache-Control', 'public, s-maxage=60, stale-while-revalidate=120');
      return res.status(200).json({ ok: true, available: false, application: app, currentVersion, latestVersion: null, mandatory: false, title: `SIRA ${app}`, notes: 'Aucune release complète contenant les deux APK n’est actuellement publiée.' });
    }

    const apk = (Array.isArray(release.assets) ? release.assets : []).find(asset => match(asset.name));
    if (!apk) {
      return res.status(200).json({ ok: true, available: false, application: app, currentVersion, latestVersion: String(release.tag_name || '').replace(/^v/i, ''), mandatory: false, title: release.name || `SIRA ${app}`, notes: 'Aucun APK correspondant à cette application dans la release complète.' });
    }

    const latestVersion = String(release.tag_name || '').replace(/^v/i, '') || '0.0.0';
    const available = isNewer(currentVersion, latestVersion);
    res.setHeader('Cache-Control', 'public, s-maxage=300, stale-while-revalidate=600');
    return res.status(200).json({
      ok: true,
      available,
      application: app,
      currentVersion,
      latestVersion,
      versionCode: Number(release.id) || 0,
      mandatory: available,
      title: release.name || `SIRA ${app} ${latestVersion}`,
      notes: release.body || '',
      apkName: apk.name,
      apkUrl: apk.browser_download_url,
      publishedAt: release.published_at || release.created_at || null
    });
  } catch (_) {
    return res.status(503).json({ ok: false, available: false, application: app, currentVersion, error: 'Service de mise à jour temporairement indisponible.' });
  }
};
