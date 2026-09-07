const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 8080;
const PUBLIC_DIR = path.join(__dirname, 'public');
const INDEX_HTML = path.join(PUBLIC_DIR, 'index.html');
const LOGO_PATH = path.join(PUBLIC_DIR, 'sira_logo.jpg');
const DATA_FILE = path.join(__dirname, 'admin_state.json');

// In-memory or persisted state (Clean state: Zero simulated data)
let state = {
  users: [
    {
      id: "sawadogoafis125_gmail_com",
      email: "sawadogoafis125@gmail.com",
      displayName: "Afis Sawadogo",
      shopName: "SIRA Direction Centrale",
      city: "Ouagadougou",
      phone: "+226 70 00 00 00",
      role: "SUPER_ADMIN",
      productCount: 0,
      saleCount: 0,
      totalRevenueFcfa: 0,
      isActive: true,
      isApproved: true,
      ifuNumber: "BF-ADMIN-001"
    }
  ],
  licenseKeys: [
    {
      key: "SIRA-VIP-BURKINA-2026",
      merchantName: "Direction Générale & Pilote",
      shopName: "SIRA Commercial VIP",
      maxUsers: 50,
      usedCount: 1,
      devices: ["sawadogoafis125_gmail_com"],
      themeColor: "#005AC1",
      appName: "SIRA Business VIP",
      logoUrl: "",
      bgUrl: "",
      cguText: "Licence officielle commerciale concédée par l'Organisation SIRA.",
      privacyText: "Données marchandes isolées et sécurisées. Souveraineté totale.",
      keypadLayout: "GRID_4",
      status: "ACTIVE", // ACTIVE or REVOKED
      createdAt: Date.now()
    }
  ],
  directives: [],
  apiKeys: {
    geminiApiKey: "",
    cinetPayApiKey: "",
    cinetPaySiteId: "",
    orangeMoneyMerchantCode: "",
    orangeMoneyApiKey: ""
  },
  logs: []
};

// Try loading persisted state
try {
  if (fs.existsSync(DATA_FILE)) {
    const loaded = JSON.parse(fs.readFileSync(DATA_FILE, 'utf-8'));
    state = { ...state, ...loaded };
  }
} catch (e) {
  console.error("Error reading admin_state.json", e);
}

function saveState() {
  try {
    fs.writeFileSync(DATA_FILE, JSON.stringify(state, null, 2), 'utf-8');
  } catch (e) {
    console.error("Error writing admin_state.json", e);
  }
}

function parseJsonBody(req, callback) {
  let body = '';
  req.on('data', chunk => { body += chunk; });
  req.on('end', () => {
    try {
      const data = body ? JSON.parse(body) : {};
      callback(null, data);
    } catch (err) {
      callback(err, null);
    }
  });
}

const server = http.createServer((req, res) => {
  const url = req.url.split('?')[0];

  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const isGet = req.method === 'GET' || req.method === 'HEAD';

  // API Endpoints
  if (url === '/api/users' && isGet) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(state.users));
    return;
  }

  if (url === '/api/users/toggle' && req.method === 'POST') {
    parseJsonBody(req, (err, data) => {
      if (data && data.userId) {
        const u = state.users.find(x => x.id === data.userId);
        if (u) {
          u.isActive = !u.isActive;
          state.logs.unshift({
            id: 'log-' + Date.now(),
            action: u.isActive ? 'USER_ACTIVATE' : 'USER_SUSPEND',
            performedBy: 'Afis Sawadogo',
            details: `Statut de ${u.displayName} (${u.shopName}) changé à ${u.isActive ? 'ACTIF' : 'SUSPENDU'}`,
            timestamp: Date.now()
          });
          saveState();
        }
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true }));
    });
    return;
  }

  if (url === '/api/users/approve' && req.method === 'POST') {
    parseJsonBody(req, (err, data) => {
      if (data && data.userId) {
        const u = state.users.find(x => x.id === data.userId);
        if (u) {
          u.isApproved = !u.isApproved;
          state.logs.unshift({
            id: 'log-' + Date.now(),
            action: u.isApproved ? 'AGREEMENT_GRANTED' : 'AGREEMENT_REVOKED',
            performedBy: 'Afis Sawadogo',
            details: `Agrément officiel de ${u.displayName} : ${u.isApproved ? 'ACCORDÉ' : 'RÉVOQUÉ'}`,
            timestamp: Date.now()
          });
          saveState();
        }
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true }));
    });
    return;
  }

  if (url === '/api/users/role' && req.method === 'POST') {
    parseJsonBody(req, (err, data) => {
      if (data && data.userId && data.role) {
        const u = state.users.find(x => x.id === data.userId);
        if (u) {
          u.role = data.role;
          state.logs.unshift({
            id: 'log-' + Date.now(),
            action: 'ROLE_CHANGE',
            performedBy: 'Afis Sawadogo',
            details: `Rôle de ${u.displayName} changé à ${data.role}`,
            timestamp: Date.now()
          });
          saveState();
        }
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true }));
    });
    return;
  }

  if (url === '/api/users/create' && req.method === 'POST') {
    parseJsonBody(req, (err, data) => {
      if (data && data.displayName && data.email) {
        const newUser = {
          id: 'user_' + Date.now().toString(36),
          email: data.email,
          displayName: data.displayName,
          shopName: data.shopName || 'Boutique Réseau',
          city: data.city || 'Ouagadougou',
          phone: data.phone || '',
          role: data.role || 'MERCHANT',
          productCount: 0,
          saleCount: 0,
          totalRevenueFcfa: 0,
          isActive: true,
          isApproved: false,
          ifuNumber: 'À renseigner'
        };
        state.users.push(newUser);
        state.logs.unshift({
          id: 'log-' + Date.now(),
          action: 'USER_REGISTERED',
          performedBy: 'Afis Sawadogo',
          details: `Enregistrement du nouveau commerçant ${data.displayName} (${data.shopName})`,
          timestamp: Date.now()
        });
        saveState();
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true }));
    });
    return;
  }

  if (url === '/api/directives' && isGet) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(state.directives));
    return;
  }

  if (url === '/api/directives' && req.method === 'POST') {
    parseJsonBody(req, (err, data) => {
      if (data && data.title && data.message) {
        const d = {
          id: 'dir-' + Date.now(),
          title: data.title,
          message: data.message,
          author: 'Afis Sawadogo (Super Admin)',
          priority: data.priority || 'INFO',
          timestamp: Date.now()
        };
        state.directives.unshift(d);
        state.logs.unshift({
          id: 'log-' + Date.now(),
          action: 'DIRECTIVE_BROADCAST',
          performedBy: 'Afis Sawadogo',
          details: `Diffusion de la directive : "${data.title}"`,
          timestamp: Date.now()
        });
        saveState();
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true }));
    });
    return;
  }

  // ============================================
  // MONETIZATION & LICENSE API KEYS
  // ============================================

  // Get all generated application license keys
  if (url === '/api/licenses' && isGet) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(state.licenseKeys || []));
    return;
  }

  // Create new monetization license key with custom quota & initial customization
  if (url === '/api/licenses/create' && req.method === 'POST') {
    parseJsonBody(req, (err, data) => {
      if (data && data.merchantName) {
        const randomPart = Math.random().toString(36).substring(2, 7).toUpperCase();
        const genKey = (data.keyPrefix ? data.keyPrefix.trim().toUpperCase() : 'SIRA') + '-' + randomPart + '-' + new Date().getFullYear();
        const newLicense = {
          key: data.key || genKey,
          merchantName: data.merchantName,
          shopName: data.shopName || data.merchantName,
          maxUsers: parseInt(data.maxUsers, 10) || 1,
          usedCount: 0,
          devices: [],
          themeColor: data.themeColor || '#005AC1',
          appName: data.appName || 'SIRA Business',
          logoUrl: data.logoUrl || '',
          bgUrl: data.bgUrl || '',
          cguText: data.cguText || "Licence officielle commerciale concédée par l'Organisation SIRA.",
          privacyText: data.privacyText || "Données marchandes isolées et sécurisées. Souveraineté totale.",
          keypadLayout: data.keypadLayout || 'GRID_4',
          status: 'ACTIVE',
          createdAt: Date.now()
        };
        if (!state.licenseKeys) state.licenseKeys = [];
        state.licenseKeys.unshift(newLicense);
        state.logs.unshift({
          id: 'log-' + Date.now(),
          action: 'LICENSE_KEY_CREATED',
          performedBy: 'Afis Sawadogo',
          details: `Clé API Licence générée: ${newLicense.key} (Quota: ${newLicense.maxUsers} utilisateur(s)) pour ${newLicense.merchantName}`,
          timestamp: Date.now()
        });
        saveState();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, license: newLicense }));
        return;
      }
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Champs manquants' }));
    });
    return;
  }

  // Revoke or Reactivate License Key
  if (url === '/api/licenses/revoke' && req.method === 'POST') {
    parseJsonBody(req, (err, data) => {
      if (data && data.key) {
        const lic = (state.licenseKeys || []).find(l => l.key === data.key);
        if (lic) {
          lic.status = (lic.status === 'ACTIVE') ? 'REVOKED' : 'ACTIVE';
          state.logs.unshift({
            id: 'log-' + Date.now(),
            action: lic.status === 'REVOKED' ? 'LICENSE_REVOKED' : 'LICENSE_REACTIVATED',
            performedBy: 'Afis Sawadogo',
            details: `Clé API ${lic.key} : Statut changé à ${lic.status}. Tout blocage client activé/désactivé.`,
            timestamp: Date.now()
          });
          saveState();
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: true, status: lic.status }));
          return;
        }
      }
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Clé introuvable' }));
    });
    return;
  }

  // Update customization of license from app or admin
  if (url === '/api/licenses/customize' && req.method === 'POST') {
    parseJsonBody(req, (err, data) => {
      if (data && data.key) {
        const lic = (state.licenseKeys || []).find(l => l.key === data.key);
        if (lic) {
          if (data.themeColor) lic.themeColor = data.themeColor;
          if (data.appName) lic.appName = data.appName;
          if (data.shopName) lic.shopName = data.shopName;
          if (data.logoUrl !== undefined) lic.logoUrl = data.logoUrl;
          if (data.bgUrl !== undefined) lic.bgUrl = data.bgUrl;
          if (data.cguText) lic.cguText = data.cguText;
          if (data.privacyText) lic.privacyText = data.privacyText;
          if (data.keypadLayout) lic.keypadLayout = data.keypadLayout;

          state.logs.unshift({
            id: 'log-' + Date.now(),
            action: 'LICENSE_CUSTOMIZED',
            performedBy: data.deviceId || 'Admin/User',
            details: `Personnalisation mise à jour pour la clé ${lic.key} (Thème: ${lic.themeColor}, Nom: ${lic.appName})`,
            timestamp: Date.now()
          });
          saveState();
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: true, license: lic }));
          return;
        }
      }
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Clé introuvable' }));
    });
    return;
  }

  // Validate License Key from Mobile APK (Enforces user quota & status)
  if (url === '/api/licenses/validate' && req.method === 'POST') {
    parseJsonBody(req, (err, data) => {
      const key = (data && data.key) ? data.key.trim() : '';
      const deviceId = (data && data.deviceId) ? data.deviceId.trim() : 'anonymous_device';

      const lic = (state.licenseKeys || []).find(l => l.key === key);
      if (!lic) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          valid: false,
          error: "Clé de licence invalide ou inexistante. Veuillez vous procurer une clé auprès de l'administrateur SIRA."
        }));
        return;
      }

      if (lic.status === 'REVOKED') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          valid: false,
          revoked: true,
          error: "Cette clé API de licence a été révoquée par l'administrateur central. L'application est immédiatement désactivée."
        }));
        return;
      }

      // Check quota of devices
      if (!lic.devices) lic.devices = [];
      const alreadyRegistered = lic.devices.includes(deviceId);

      if (!alreadyRegistered) {
        if (lic.devices.length >= lic.maxUsers) {
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            valid: false,
            error: `Quota maximal d'utilisateurs atteint (${lic.maxUsers}/${lic.maxUsers} appareils autorisés). Contactez l'administrateur pour augmenter votre capacité.`
          }));
          return;
        }
        lic.devices.push(deviceId);
        lic.usedCount = lic.devices.length;
        saveState();
      }

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        valid: true,
        license: lic
      }));
    });
    return;
  }

  if (url === '/api/keys' && isGet) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(state.apiKeys));
    return;
  }

  if (url === '/api/keys' && req.method === 'POST') {
    parseJsonBody(req, (err, data) => {
      if (data) {
        state.apiKeys = { ...state.apiKeys, ...data };
        state.logs.unshift({
          id: 'log-' + Date.now(),
          action: 'API_KEYS_UPDATED',
          performedBy: 'Afis Sawadogo',
          details: `Mise à jour sécurisée du trousseau de clés API`,
          timestamp: Date.now()
        });
        saveState();
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true }));
    });
    return;
  }

  if (url === '/api/logs' && isGet) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(state.logs.slice(0, 50)));
    return;
  }

  if (url === '/api/system' && isGet) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      serverStatus: 'Portail Cloud Central SIRA (Actif)',
      serverTime: new Date().toISOString(),
      merchantsCount: state.users.length,
      directivesCount: state.directives.length,
      platform: 'Organisation SIRA - Burkina Faso'
    }));
    return;
  }

  // Serve Logo
  if (url === '/sira_logo.jpg' || url === '/logo.jpg') {
    if (fs.existsSync(LOGO_PATH)) {
      res.writeHead(200, { 'Content-Type': 'image/jpeg', 'Cache-Control': 'public, max-age=86400' });
      fs.createReadStream(LOGO_PATH).pipe(res);
      return;
    }
  }

  // Health check
  if (url === '/health' || url === '/ping') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('OK');
    return;
  }

  // Default: Serve SPA Admin Portal index.html for ALL web routes!
  if (fs.existsSync(INDEX_HTML)) {
    res.writeHead(200, {
      'Content-Type': 'text/html; charset=utf-8',
      'Cache-Control': 'no-cache, no-store, must-revalidate'
    });
    fs.createReadStream(INDEX_HTML).pipe(res);
  } else {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(`<!DOCTYPE html><html><head><title>Organisation SIRA</title></head><body style="background:#0b1120;color:white;font-family:sans-serif;padding:40px;text-align:center;"><h1>Organisation SIRA</h1><p>Direction Générale • Chemin d'aujourd'hui, Avenir de demain</p></body></html>`);
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`SIRA Admin Portal & Cloud Server listening on port ${PORT}`);
});
