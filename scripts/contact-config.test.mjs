import { test } from 'node:test';
import assert from 'node:assert/strict';
import { productionContactConfig } from './configure-contact.mjs';

test('missing backend URL permits a portfolio-only deployment', () => {
  for (const value of [undefined, '', ' ']) {
    assert.deepEqual(productionContactConfig(value), { apiBaseUrl: '' });
  }
});

test('configured backend still requires HTTPS and rejects localhost', () => {
  for (const value of ['http://api.example.com', 'https://localhost', 'https://127.0.0.1', 'https://test.localhost']) {
    assert.throws(() => productionContactConfig(value));
  }
});

test('browser config never accepts embedded credentials or an endpoint path', () => {
  for (const value of ['https://user:password@api.example.com', 'https://api.example.com/api/contact',
    'https://api.example.com/?key=secret', 'https://api.example.com/#secret', 'not-a-url']) {
    assert.throws(() => productionContactConfig(value));
  }
});

test('production URL is normalized so form requests target the backend origin', () => {
  assert.deepEqual(productionContactConfig(' https://api.example.com/ '), { apiBaseUrl: 'https://api.example.com' });
});
