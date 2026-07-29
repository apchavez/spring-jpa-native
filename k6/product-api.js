import http from 'k6/http';
import { check, group, sleep } from 'k6';

// ─── Configuration ────────────────────────────────────────────────────────────

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN    = __ENV.API_TOKEN || '';

const STAGES = {
  smoke: [
    { duration: '30s', target: 1 },
  ],
  load: [
    { duration: '30s', target:  5 },
    { duration: '1m',  target: 20 },
    { duration: '30s', target:  0 },
  ],
  spike: [
    { duration: '10s', target: 50 },
    { duration: '30s', target: 50 },
    { duration: '10s', target:  0 },
  ],
};

export const options = {
  stages: STAGES[__ENV.SCENARIO || 'smoke'],
  thresholds: {
    http_req_failed:                       ['rate<0.01'],
    http_req_duration:                     ['p(95)<500', 'p(99)<1000'],
    'group_duration{group:::crud}':        ['p(95)<2000'],
    'group_duration{group:::read-paths}':  ['p(95)<300'],
  },
};

// ─── Helpers ─────────────────────────────────────────────────────────────────

const HEADERS = {
  'Content-Type': 'application/json',
  Authorization: `Bearer ${TOKEN}`,
};

function randomSuffix() {
  return Math.random().toString(36).slice(2, 8).toUpperCase();
}

function buildProduct(sku, name) {
  return JSON.stringify({
    sku,
    name,
    description: 'k6 load test product',
    categoryId:  1,
    price:       99.99,
    stock:       100,
    active:      true,
  });
}

// ─── Main scenario ────────────────────────────────────────────────────────────

export default function () {
  const suffix = randomSuffix();
  const sku    = `SKU-K6-${suffix}`;
  const name   = `LoadProduct ${suffix}`;

  group('crud', () => {
    // POST — create
    const createRes = http.post(
      `${BASE_URL}/api/v1/products`,
      buildProduct(sku, name),
      { headers: HEADERS }
    );
    const created = check(createRes, {
      'POST /products → 201':  r => r.status === 201,
      'POST response has id':  r => r.json('id') !== undefined,
    });
    if (!created) return;

    const id = createRes.json('id');

    // GET by ID
    const getRes = http.get(`${BASE_URL}/api/v1/products/${id}`, { headers: HEADERS });
    check(getRes, {
      'GET /products/{id} → 200': r => r.status === 200,
      'GET response sku matches': r => r.json('sku') === sku,
    });

    // PUT — update
    const putRes = http.put(
      `${BASE_URL}/api/v1/products/${id}`,
      buildProduct(sku, `Updated ${suffix}`),
      { headers: HEADERS }
    );
    check(putRes, { 'PUT /products/{id} → 200': r => r.status === 200 });

    // DELETE
    const delRes = http.del(
      `${BASE_URL}/api/v1/products/${id}`,
      null,
      { headers: HEADERS }
    );
    check(delRes, { 'DELETE /products/{id} → 204 or 200': r => r.status === 204 || r.status === 200 });
  });

  group('read-paths', () => {
    // List active products (paginated)
    const listRes = http.get(
      `${BASE_URL}/api/v1/products/active?page=0&size=10`,
      { headers: HEADERS }
    );
    check(listRes, { 'GET /products/active → 200': r => r.status === 200 });

    // Search by name prefix
    const searchRes = http.get(
      `${BASE_URL}/api/v1/products/search?prefix=Load`,
      { headers: HEADERS }
    );
    check(searchRes, { 'GET /products/search → 200': r => r.status === 200 });

    // Inventory summary report
    const reportRes = http.get(
      `${BASE_URL}/api/v1/reports/inventory-summary`,
      { headers: HEADERS }
    );
    check(reportRes, { 'GET /reports/inventory-summary → 200': r => r.status === 200 });
  });

  sleep(0.5);
}
