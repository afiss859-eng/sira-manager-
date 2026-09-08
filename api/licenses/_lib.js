const crypto = require('node:crypto');

const VERSION = 2;
const PREFIX = 'SIRA-LIC';
const MODELS = ['BOUTIQUE', 'NATIONAL', 'INTERNATIONAL'];
const KEYPADS = ['GRID_4', 'GRID_2', 'LIST_COMPACT'];
const SALT = 'SIRA-MANAGER-LICENSE-V2';

function base64url(value) {
  return Buffer.from(value, 'utf8').toString('base64url');
}

function fromBase64url(value) {
  return Buffer.from(value, 'base64url').toString('utf8');
}

function sign(payload) {
  return crypto.createHash('sha256').update(`${SALT}|${payload}`).digest('hex').slice(0, 16).toUpperCase();
}

function profileFor(model) {
  const national = model === 'NATIONAL' || model === 'INTERNATIONAL';
  const international = model === 'INTERNATIONAL';
  return {
    stockModel: model,
    barcode: true,
    multiStore: national,
    multiWarehouse: national,
    locations: national,
    lots: national,
    serialNumbers: national,
    expiry: national,
    transfers: national,
    purchaseSuggestions: national,
    forecasting: international,
    international,
    advancedAudit: national,
    importExport: international,
    landedCost: international,
    demandForecasting: international,
    offlineFirst: true,
    bluetoothPrint: true,
    proforma: true,
    returns: true
  };
}

function cleanString(value, fallback = '', max = 160) {
  const text = String(value ?? '').trim();
  return (text || fallback).slice(0, max);
}

function createLicense(input = {}) {
  const stockModel = MODELS.includes(String(input.stockModel).toUpperCase()) ? String(input.stockModel).toUpperCase() : 'BOUTIQUE';
  const maxUsers = Math.max(1, Math.min(100000, Number(input.maxUsers) || 1));
  const payloadObject = {
    v: VERSION,
    id: crypto.randomBytes(9).toString('hex').toUpperCase(),
    merchantName: cleanString(input.merchantName, 'Commerce SIRA'),
    shopName: cleanString(input.shopName, 'SIRA Business'),
    maxUsers,
    stockModel,
    appName: cleanString(input.appName, 'SIRA Business'),
    profilePhotoUrl: cleanString(input.profilePhotoUrl, '', 500),
    logoUrl: cleanString(input.logoUrl, '', 500),
    bgUrl: cleanString(input.bgUrl, '', 500),
    themeColor: /^#[0-9A-Fa-f]{6}$/.test(String(input.themeColor || '')) ? String(input.themeColor).toUpperCase() : '#005AC1',
    keypadLayout: KEYPADS.includes(String(input.keypadLayout).toUpperCase()) ? String(input.keypadLayout).toUpperCase() : 'GRID_4',
    country: cleanString(input.country, 'Burkina Faso', 80),
    currency: cleanString(input.currency, 'XOF', 10),
    cguText: cleanString(input.cguText, 'Licence officielle SIRA.', 500),
    privacyText: cleanString(input.privacyText, 'Données protégées et isolées.', 500),
    createdAt: new Date().toISOString()
  };
  const payload = base64url(JSON.stringify(payloadObject));
  return `${PREFIX}-${payload}.${sign(payload)}`;
}

function decodeLicense(key) {
  const input = String(key || '').trim();
  if (!input.startsWith(`${PREFIX}-`)) return null;
  const body = input.slice(PREFIX.length + 1);
  const dot = body.lastIndexOf('.');
  if (dot <= 0) return null;
  const payload = body.slice(0, dot);
  const signature = body.slice(dot + 1).toUpperCase();
  if (sign(payload) !== signature) return null;
  try {
    const data = JSON.parse(fromBase64url(payload));
    if (!data || data.v !== VERSION || !MODELS.includes(data.stockModel)) return null;
    data.maxUsers = Math.max(1, Number(data.maxUsers) || 1);
    data.features = profileFor(data.stockModel);
    return data;
  } catch (_) {
    return null;
  }
}

module.exports = { MODELS, KEYPADS, profileFor, createLicense, decodeLicense };
