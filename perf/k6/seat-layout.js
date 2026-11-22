// =====================================================================
// 좌석 배치도 조회 API 부하 테스트
//   GET /concerts/{concertId}/schedules/{scheduleId}/seats[?sectionId=]
//
// 기존 JMeter 조건과 맞춤: 50명(VU)이 각 5번 = 총 250 요청
//
// 실행 예:
//   k6 run -e SCHEDULE_ID=5 perf/k6/seat-layout.js                 # 전체 30,000석
//   k6 run -e SCHEDULE_ID=5 -e SECTION_ID=13 perf/k6/seat-layout.js  # 구역 10,000석
// =====================================================================
import http from 'k6/http';
import { check, fail } from 'k6';

// ---- 실행 시 -e 로 바꿀 수 있는 값 ----
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CONCERT_ID = __ENV.CONCERT_ID || '1';
const SCHEDULE_ID = __ENV.SCHEDULE_ID;               // 필수 (시드 SQL 결과 참고)
const SECTION_ID = __ENV.SECTION_ID;                 // 없으면 전체 구역 조회
const EMAIL = __ENV.EMAIL || 'perf@ticksy.com';
const PASSWORD = __ENV.PASSWORD || 'Perf1234!';
const VUS = Number(__ENV.VUS || 50);
const ITERATIONS = Number(__ENV.ITERATIONS || 5);

export const options = {
  scenarios: {
    seat_layout: {
      executor: 'per-vu-iterations', // 각 VU가 정해진 횟수만큼 반복 (JMeter 스레드 x 반복과 같은 방식)
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: '30m',
    },
  },
  // 결과 요약에 보여줄 통계: 평균만 보지 말고 p95, p99도 같이 본다
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'], // 실패율 1% 넘으면 테스트 실패로 표시
    // 아래 두 줄은 합격 기준이 아니라 "로그인 요청을 뺀 좌석 조회만의 통계"를
    // 결과 요약에 따로 표시하려는 용도 (조건이 항상 참이라 실패하지 않음)
    'http_req_duration{name:seat_layout_all}': ['max>=0'],
    'http_req_duration{name:seat_layout_section}': ['max>=0'],
  },
};

// setup: 테스트 시작 전에 한 번만 실행 → 로그인해서 토큰 받기
export function setup() {
  if (!SCHEDULE_ID) {
    fail('SCHEDULE_ID가 필요합니다. 예: k6 run -e SCHEDULE_ID=5 perf/k6/seat-layout.js');
  }

  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  const token = res.status === 200 ? res.json('data.accessToken') : null;
  if (!token) {
    fail(`로그인 실패: status=${res.status} body=${res.body}`);
  }
  return { token };
}

// default: 각 VU가 반복 실행하는 본문
export default function (data) {
  let url = `${BASE_URL}/concerts/${CONCERT_ID}/schedules/${SCHEDULE_ID}/seats`;
  if (SECTION_ID) {
    url += `?sectionId=${SECTION_ID}`;
  }

  const res = http.get(url, {
    headers: { Authorization: `Bearer ${data.token}` },
    timeout: '120s', // 기본값 60초. 개선 전에는 60초를 넘을 수 있어서 늘림
    tags: { name: SECTION_ID ? 'seat_layout_section' : 'seat_layout_all' },
  });

  check(res, {
    'status 200': (r) => r.status === 200,
    'success true': (r) => r.status === 200 && r.json('success') === true,
  });
}
