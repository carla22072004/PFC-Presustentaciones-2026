import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 20 }, // Ramp-up to 20 users
        { duration: '1m', target: 50 },  // Stay at 50 users (peak)
        { duration: '30s', target: 0 },  // Ramp-down to 0
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests under 500ms
        http_req_failed: ['rate<0.01'],   // Less than 1% errors
    },
};

const BASE_URL = 'http://localhost:8080/api';

export default function () {
    // 1. Health check / catalog request
    const catalogRes = http.get(`${BASE_URL}/catalogos/carreras`);
    check(catalogRes, {
        'status is 200 or 401': (r) => r.status === 200 || r.status === 401,
    });

    // 2. Auth Login test
    const payload = JSON.stringify({
        email: 'admin@uteq.edu.ec',
        password: 'password123',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const loginRes = http.post(`${BASE_URL}/auth/login`, payload, params);
    check(loginRes, {
        'login status is 200 or 400': (r) => r.status === 200 || r.status === 400,
    });

    sleep(1);
}
