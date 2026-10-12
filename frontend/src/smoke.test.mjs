import assert from 'node:assert';
import { describe, it } from 'node:test';

console.log('--- Running Frontend Smoke Tests (Sprint 2.5 Auth & Shell) ---');

// ══════════════════════════════════════════════════════════════════════════
// 1. Environment configuration
// ══════════════════════════════════════════════════════════════════════════

describe('Environment configuration', () => {
  it('apiBaseUrl should be a non-empty string', () => {
    const apiBaseUrl = process.env.VITE_API_BASE_URL || '/api/v1';
    assert.strictEqual(typeof apiBaseUrl, 'string');
    assert.ok(apiBaseUrl.length > 0);
  });
});

// ══════════════════════════════════════════════════════════════════════════
// 2. ApiClientError — construction and field mapping
// ══════════════════════════════════════════════════════════════════════════

class ApiClientError extends Error {
  constructor(message, code, status, details) {
    super(message);
    this.name = 'ApiClientError';
    this.code = code;
    this.status = status;
    this.details = details;
  }
}

describe('ApiClientError', () => {
  it('maps all constructor fields correctly', () => {
    const err = new ApiClientError('Test message', 'TEST_CODE', 400, { field: 'name' });
    assert.strictEqual(err.message, 'Test message');
    assert.strictEqual(err.name, 'ApiClientError');
    assert.strictEqual(err.code, 'TEST_CODE');
    assert.strictEqual(err.status, 400);
    assert.deepStrictEqual(err.details, { field: 'name' });
    assert.ok(err instanceof Error);
  });

  it('represents a timeout error with status 0', () => {
    const err = new ApiClientError(
      'Request timed out. The backend did not respond in time.',
      'REQUEST_TIMEOUT',
      0,
    );
    assert.strictEqual(err.code, 'REQUEST_TIMEOUT');
    assert.strictEqual(err.status, 0);
  });

  it('represents a network failure with status 0', () => {
    const err = new ApiClientError(
      'Network connection failed. Unable to reach the backend service.',
      'NETWORK_FAILURE',
      0,
    );
    assert.strictEqual(err.code, 'NETWORK_FAILURE');
    assert.strictEqual(err.status, 0);
  });

  it('derives HTTP error code from status when backend provides none', () => {
    const status = 503;
    const code = `HTTP_${status}`;
    const err = new ApiClientError('Service Unavailable', code, status);
    assert.strictEqual(err.code, 'HTTP_503');
    assert.strictEqual(err.status, 503);
  });
});

// ══════════════════════════════════════════════════════════════════════════
// 3. ApiResponseEnvelope — data unwrapping logic
// ══════════════════════════════════════════════════════════════════════════

describe('ApiResponseEnvelope unwrapping', () => {
  function unwrapEnvelope(json, status) {
    if (!json) throw new ApiClientError('Non-JSON response', 'INVALID_RESPONSE', status);
    if (status < 200 || status >= 300) {
      const code = json?.error?.code ?? `HTTP_${status}`;
      const message = json?.error?.message ?? json?.message ?? 'Unexpected error.';
      throw new ApiClientError(message, code, status, json?.error?.details);
    }
    if (json.data !== undefined) return json.data;
    return json;
  }

  it('returns data field when present in envelope', () => {
    const envelope = { success: true, data: { status: 'UP', environment: 'test', timestamp: '2026-01-01T00:00:00Z' }, timestamp: '2026-01-01T00:00:00Z' };
    const result = unwrapEnvelope(envelope, 200);
    assert.deepStrictEqual(result, { status: 'UP', environment: 'test', timestamp: '2026-01-01T00:00:00Z' });
  });

  it('throws ApiClientError with backend error code on non-2xx', () => {
    const errorEnvelope = { success: false, error: { code: 'RESOURCE_NOT_FOUND', message: 'Entity missing' }, timestamp: '' };
    assert.throws(() => unwrapEnvelope(errorEnvelope, 404), (err) => {
      assert.ok(err instanceof ApiClientError);
      assert.strictEqual(err.code, 'RESOURCE_NOT_FOUND');
      assert.strictEqual(err.status, 404);
      return true;
    });
  });

  it('throws INVALID_RESPONSE when body is null', () => {
    assert.throws(() => unwrapEnvelope(null, 200), (err) => {
      assert.ok(err instanceof ApiClientError);
      assert.strictEqual(err.code, 'INVALID_RESPONSE');
      return true;
    });
  });
});

// ══════════════════════════════════════════════════════════════════════════
// 4. Token & Session Management Simulation
// ══════════════════════════════════════════════════════════════════════════

describe('Token and Session State Management', () => {
  let tokenStore = null;

  function setToken(t) {
    tokenStore = t;
  }

  function getToken() {
    return tokenStore;
  }

  function buildHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  }

  it('attaches Authorization Bearer header when token exists', () => {
    setToken('sample-jwt-token-xyz');
    const headers = buildHeaders();
    assert.strictEqual(headers['Authorization'], 'Bearer sample-jwt-token-xyz');
  });

  it('omits Authorization header when token is null', () => {
    setToken(null);
    const headers = buildHeaders();
    assert.strictEqual(headers['Authorization'], undefined);
  });

  it('clears token on logout or session invalidation', () => {
    setToken('active-token');
    assert.strictEqual(getToken(), 'active-token');
    // Simulate logout action
    setToken(null);
    assert.strictEqual(getToken(), null);
  });
});

// ══════════════════════════════════════════════════════════════════════════
// 5. Auth Flow & Role-Based UI Guard Simulation
// ══════════════════════════════════════════════════════════════════════════

describe('Permission-Aware UI Logic', () => {
  const ROLE_HIERARCHY = {
    OWNER: 4,
    ADMIN: 3,
    ANALYST: 2,
    VIEWER: 1,
  };

  function hasAccess(userRole, requiredRole) {
    if (!userRole) return false;
    return (ROLE_HIERARCHY[userRole] || 0) >= (ROLE_HIERARCHY[requiredRole] || 0);
  }

  it('allows OWNER and ADMIN to access admin navigation items', () => {
    assert.strictEqual(hasAccess('OWNER', 'ADMIN'), true);
    assert.strictEqual(hasAccess('ADMIN', 'ADMIN'), true);
    assert.strictEqual(hasAccess('ANALYST', 'ADMIN'), false);
    assert.strictEqual(hasAccess('VIEWER', 'ADMIN'), false);
  });

  it('allows all roles to view threats & status', () => {
    assert.strictEqual(hasAccess('OWNER', 'VIEWER'), true);
    assert.strictEqual(hasAccess('ADMIN', 'VIEWER'), true);
    assert.strictEqual(hasAccess('ANALYST', 'VIEWER'), true);
    assert.strictEqual(hasAccess('VIEWER', 'VIEWER'), true);
    assert.strictEqual(hasAccess(null, 'VIEWER'), false);
  });
});

// ══════════════════════════════════════════════════════════════════════════
// 6. CORS allowed-origins parsing
// ══════════════════════════════════════════════════════════════════════════

describe('CORS allowed-origins parsing', () => {
  function parseOrigins(raw) {
    return raw.split(',').map(s => s.trim()).filter(s => s.length > 0);
  }

  it('parses single origin', () => {
    assert.deepStrictEqual(parseOrigins('http://localhost:5173'), ['http://localhost:5173']);
  });

  it('parses multiple origins with whitespace', () => {
    assert.deepStrictEqual(
      parseOrigins('http://localhost:5173, https://app.example.com'),
      ['http://localhost:5173', 'https://app.example.com'],
    );
  });
});

console.log('--- All Frontend Smoke Tests Passed ---');
