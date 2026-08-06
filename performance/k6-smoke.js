import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    smoke: { executor: 'constant-vus', vus: Number(__ENV.VUS || 2), duration: __ENV.DURATION || '30s' },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1500'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const headers = {
  Authorization: `Bearer ${__ENV.AUTH_TOKEN || ''}`,
  'X-Shop-Id': __ENV.SHOP_ID || '1',
};

export default function () {
  const endpoints = ['/api/dashboard/overview', '/api/orders?page=1&pageSize=20', '/api/agent/tasks?page=1&pageSize=20'];
  for (const endpoint of endpoints) {
    const response = http.get(`${baseUrl}${endpoint}`, { headers, tags: { endpoint } });
    check(response, { 'status is not 5xx': (r) => r.status < 500 });
  }
  sleep(1);
}
