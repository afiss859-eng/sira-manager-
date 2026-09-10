const test = require('node:test');
const assert = require('node:assert/strict');

process.env.SIRA_ADMIN_SECRET = 'test-admin-secret-012345678901234567890123456789';

const { issueSession, isAdmin, sessionCookie, clearCookie } = require('./_auth');

test('signed admin session is accepted and cookie is hardened', () => {
  const token = issueSession();
  assert.match(token, /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/);
  assert.equal(isAdmin({ headers: { cookie: sessionCookie(token) } }), true);
  assert.match(sessionCookie(token), /HttpOnly/);
  assert.match(sessionCookie(token), /Secure/);
  assert.match(sessionCookie(token), /SameSite=Strict/);
  assert.match(clearCookie(), /Max-Age=0/);
});

test('missing or tampered session is rejected', () => {
  assert.equal(isAdmin({ headers: { cookie: '' } }), false);
  assert.equal(isAdmin({ headers: { cookie: 'sira_admin_session=bad.token' } }), false);
});
