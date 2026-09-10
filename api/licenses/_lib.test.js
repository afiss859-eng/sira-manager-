const test = require('node:test');
const assert = require('node:assert/strict');

process.env.SIRA_LICENSE_SECRET = 'sira-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz';

const { createLicense, decodeLicense, profileFor } = require('./_lib');

test('creates and decodes a signed Business license', () => {
  const key = createLicense({
    merchantName: 'Commerce Test',
    shopName: 'Boutique Test',
    maxUsers: 7,
    stockModel: 'NATIONAL',
    durationDays: 90,
    currency: 'XOF'
  });

  assert.match(key, /^SIRA-LIC-.+\.[A-F0-9]{32}$/);
  const license = decodeLicense(key);

  assert.ok(license);
  assert.equal(license.v, 3);
  assert.equal(license.merchantName, 'Commerce Test');
  assert.equal(license.shopName, 'Boutique Test');
  assert.equal(license.maxUsers, 7);
  assert.equal(license.stockModel, 'NATIONAL');
  assert.equal(license.durationDays, 90);
  assert.equal(license.expired, false);
  assert.equal(license.currency, 'XOF');
});

test('rejects tampered license payload and signature', () => {
  const key = createLicense({ merchantName: 'Integrity Test' });
  const [head, signature] = key.split('.');
  assert.equal(decodeLicense(`${head.slice(0, -1)}X.${signature}`), null);
  assert.equal(decodeLicense(`${head}.${signature.slice(0, -1)}0`), null);
});

test('normalizes invalid commercial configuration safely', () => {
  const key = createLicense({
    maxUsers: -20,
    stockModel: 'UNKNOWN',
    durationDays: 999999,
    keypadLayout: 'UNKNOWN'
  });
  const license = decodeLicense(key);

  assert.ok(license);
  assert.equal(license.maxUsers, 1);
  assert.equal(license.stockModel, 'BOUTIQUE');
  assert.equal(license.durationDays, 3650);
  assert.equal(license.keypadLayout, 'GRID_4');
});

test('feature profile changes by stock model', () => {
  const boutique = profileFor('BOUTIQUE');
  const national = profileFor('NATIONAL');
  const international = profileFor('INTERNATIONAL');

  assert.equal(boutique.multiStore, false);
  assert.equal(national.multiStore, true);
  assert.equal(national.multiWarehouse, true);
  assert.equal(international.international, true);
  assert.equal(international.importExport, true);
});
