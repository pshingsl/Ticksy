# Ticksy 성능 측정 가이드 (k6)

좌석 배치도 조회 API의 **개선 전 기준선**을 측정하기 위한 문서입니다.
이 숫자가 이후 모든 개선의 비교 기준이 되므로, 조건을 바꾸지 않고 같은 방식으로 반복 측정합니다.

```
perf/
├── README.md                    ← 이 문서
├── seed/seed-30000-seats.sql    ← 30,000석 측정 데이터
└── k6/seat-layout.js            ← 부하 테스트 스크립트
```

---

## 1. k6란?

부하 테스트 도구입니다. JMeter와 하는 일은 같지만 차이가 있습니다.

| | JMeter | k6 |
|---|---|---|
| 시나리오 작성 | GUI에서 클릭 | JavaScript 코드 |
| 버전 관리 | XML 파일 (diff 보기 어려움) | `.js` 파일 (Git으로 변경 이력 관리) |
| 실행 | GUI 또는 CLI | CLI 한 줄 |
| 자원 사용 | 무거움 (JVM) | 가벼움 (Go) |

꼭 알아야 할 용어 4개:

- **VU (Virtual User)**: 가상 사용자. JMeter의 "스레드"와 같습니다.
- **iteration**: VU가 `default function`을 한 번 실행하는 것. JMeter의 "반복 횟수"와 같습니다.
- **executor**: VU와 반복을 어떻게 돌릴지 정하는 방식. 여기서는 `per-vu-iterations`(VU마다 정해진 횟수 반복)를 씁니다.
- **setup()**: 테스트 시작 전에 **한 번만** 실행되는 함수. 여기서는 로그인해서 토큰을 받는 데 씁니다.

---

## 2. 설치

### Windows
PowerShell을 열고 둘 중 하나:
```powershell
winget install k6 --source winget
# 또는 (Chocolatey를 쓰는 경우)
choco install k6
```
설치 후 **PowerShell을 새로 열어야** `k6` 명령이 인식됩니다.

### macOS
```bash
brew install k6
```

### 설치 확인
```bash
k6 version
```
`k6 v1.x.x ...`가 나오면 성공입니다.

> Docker로도 실행할 수 있지만(`grafana/k6` 이미지), 컨테이너 안에서는 `localhost`가 내 PC가 아니라서
> `BASE_URL=http://host.docker.internal:8080`으로 바꿔야 합니다. 처음에는 직접 설치를 권장합니다.

---

## 3. 측정 전 준비

### 3-1. 기본 데이터가 있는지 확인
측정용 회차는 `concert_id = 1`(기존 `data.sql`의 BTS 공연)에 붙습니다.
기존에 애플리케이션을 한 번이라도 띄웠다면 이미 있습니다.

### 3-2. 30,000석 측정 데이터 넣기
MySQL Workbench나 CLI에서 `perf/seed/seed-30000-seats.sql`을 **통째로** 실행합니다.

```bash
mysql -u root -p <DB이름> < perf/seed/seed-30000-seats.sql
```

마지막 SELECT 결과를 **메모**하세요. k6 실행에 그대로 씁니다.

```
schedule_id | section_id | name  | seat_count
5           | 13         | A구역 | 10000
5           | 14         | B구역 | 10000
5           | 15         | C구역 | 10000
```
(숫자는 DB 상태에 따라 다릅니다)

이 스크립트는 측정 계정 `perf@ticksy.com / Perf1234!`도 만듭니다.
회원가입은 이메일 인증이 필요해서, 비밀번호를 BCrypt로 암호화한 값을 직접 넣었습니다.

### 3-3. 측정 프로파일로 서버 실행
```bash
cd backend/backend
./gradlew bootRun --args='--spring.profiles.active=perf'
```
Windows PowerShell에서는:
```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=perf'
```

`perf` 프로파일(`application-perf.yml`)이 바꾸는 것:
- SQL 콘솔 출력 끄기 → 로그 출력이 측정값을 오염시키지 않도록
- `data.sql` 자동 실행 끄기 → 재시작할 때마다 데이터가 바뀌지 않도록
- `[PERF]` 구간 측정 로그만 출력

### 3-4. Redis 상태 맞추기
측정할 때마다 선점 데이터가 같은 상태여야 합니다. 측정 전에 선점 키를 비웁니다.
```bash
redis-cli --scan --pattern "seat:hold:*" | xargs -r redis-cli del
```
(Windows에서 Docker로 Redis를 띄웠다면 `docker exec -it <컨테이너명> redis-cli FLUSHDB`. 다른 데이터도 지워지니 주의)

---

## 4. 실행

프로젝트 루트(`Ticksy/`)에서 실행합니다. `SCHEDULE_ID`, `SECTION_ID`는 3-2에서 메모한 값으로 바꾸세요.

```bash
# (1) 구역 1개 = 10,000석 조회
k6 run -e SCHEDULE_ID=5 -e SECTION_ID=13 --summary-export=perf/results/before-section.json perf/k6/seat-layout.js

# (2) 전체 = 30,000석 조회 (sectionId 없음)
k6 run -e SCHEDULE_ID=5 --summary-export=perf/results/before-all.json perf/k6/seat-layout.js
```

> `perf/results` 폴더가 없으면 먼저 만드세요. (`mkdir perf/results`)

바꿀 수 있는 옵션 (`-e 이름=값`):

| 옵션 | 기본값 | 설명 |
|---|---|---|
| `SCHEDULE_ID` | (필수) | 측정할 회차 |
| `SECTION_ID` | 없음 | 있으면 해당 구역만, 없으면 전체 조회 |
| `VUS` | 50 | 동시 사용자 수 |
| `ITERATIONS` | 5 | 사용자당 반복 횟수 |
| `BASE_URL` | `http://localhost:8080` | 서버 주소 |

기본값 50 × 5 = 250 요청은 **기존 JMeter 측정과 같은 조건**입니다.

처음에는 작게 돌려서 동작부터 확인하세요:
```bash
k6 run -e SCHEDULE_ID=5 -e SECTION_ID=13 -e VUS=1 -e ITERATIONS=1 perf/k6/seat-layout.js
```

---

## 5. 결과 읽는 법

실행이 끝나면 이런 요약이 나옵니다.

```
✓ status 200
✓ success true

http_req_duration..............: avg=...  (로그인 요청 포함 전체)
  { name:seat_layout_section }...: avg=21.9s min=... med=... p(90)=... p(95)=... p(99)=... max=...
http_req_failed................: 0.00%  0 out of 251
http_reqs......................: 251    3.2/s
```

**볼 줄은 `{ name:seat_layout_section }` 또는 `{ name:seat_layout_all }`입니다.**
그냥 `http_req_duration`에는 setup의 로그인 요청 1건이 섞여 있습니다.

| 지표 | 의미 | 왜 보나 |
|---|---|---|
| `avg` | 평균 | 기존 JMeter 값과 비교용. 하지만 평균만 보면 느린 요청이 가려집니다 |
| `med` | 중앙값 | 보통 사용자가 겪는 시간 |
| `p(95)` | 95%의 요청이 이 시간 안에 끝남 | **실무에서 가장 많이 보는 기준** |
| `p(99)` | 99%가 이 안에 끝남 | 가장 운 나쁜 사용자 쪽 |
| `max` | 가장 느린 요청 | 튀는 값 확인 |
| `http_req_failed` | 실패율 | 0%가 아니면 시간 값을 믿으면 안 됩니다 (타임아웃, 500 에러) |
| `http_reqs` | 총 요청 수와 초당 처리량(TPS) | 처리 능력 |
| `checks` | 응답 검증 통과율 | 200인데 내용이 잘못된 경우를 잡음 |

---

## 6. 서버 로그로 "어디서" 느린지 확인

k6는 **얼마나** 느린지 알려주고, 서버 로그는 **어디서** 느린지 알려줍니다.
측정 중 서버 콘솔에 요청마다 이런 로그가 찍힙니다.

```
[PERF] seatLayout scheduleId=5 sectionId=13 seats=10000 redisCalls=10000 serviceMs=15873
StopWatch 'seatLayout': 15873 ms
----------------------------------------
ms          %       Task name
----------------------------------------
2           0%      1. 회차 조회 (DB)
210         1%      2. 구역+좌석 조회 (DB, fetch join)
15570       98%     3. 선점 상태 조회 (Redis, 좌석마다 1회)
80          1%      4. DTO 변환
11          0%      5. 탭용 구역 목록 조회 (DB)

[PERF] serverMs=16120 status=200 uri=/concerts/1/schedules/5/seats?sectionId=13
```
(위 숫자는 예시입니다. 실제 값을 기록하세요)

- `serviceMs`: 서비스 메서드 안에서 쓴 시간 (구간별로 나뉨)
- `serverMs`: 서버 전체 처리 시간
- **`serverMs − serviceMs` ≈ 트랜잭션 커밋(변경 감지) + JSON 직렬화 시간**
- `k6 시간 − serverMs` ≈ 네트워크, 요청 대기열, JWT 인증 필터 시간

이 세 숫자로 "DB인가, Redis인가, 직렬화인가"를 **추측이 아니라 숫자로** 판단합니다.

---

## 7. 인덱스 상태 확인 (EXPLAIN)

트러블슈팅 문서의 "인덱스 추가로 30.9% 개선"이 진짜였는지 확인합니다.

```sql
-- seats 테이블에 어떤 인덱스가 있는지
SHOW INDEX FROM seats;

-- 좌석 조회 쿼리가 인덱스를 쓰는지
EXPLAIN
SELECT s.*, st.*
FROM sections s
JOIN seats st ON st.section_id = s.section_id
WHERE s.schedule_id = 5 AND s.section_id = 13;
```

확인할 것:
- `SHOW INDEX` 결과에 `uk_section_row_col`(맨 앞 컬럼이 `section_id`), FK 인덱스, `idx_section_id`가 **함께** 있다면 중복 인덱스입니다.
- `EXPLAIN`의 `seats` 행에서 `type`이 `ref`이고 `key`에 인덱스 이름이 있으면 인덱스를 쓰는 중입니다. `ALL`이면 전체 스캔입니다.
- `idx_section_id`를 지운 상태와 있는 상태 모두 `type = ref`라면, 30.9% 개선은 인덱스 효과가 아니었다는 뜻입니다.

결과 화면을 캡처해서 남기세요. 트러블슈팅 문서의 근거가 됩니다.

---

## 8. 측정 규칙 (숫자를 믿을 수 있게)

1. **워밍업 1회 후 본 측정 3회**. 첫 실행은 JVM 최적화(JIT)와 DB 캐시 때문에 느립니다. 3회의 중앙값을 기록합니다.
2. **조건 고정**: 같은 데이터, 같은 VU/반복 수, 같은 프로파일, Redis 선점 키 비운 상태.
3. **다른 프로그램 끄기**: IDE 인덱싱, 브라우저 탭, 빌드 등은 CPU를 나눠 씁니다.
4. **한계 기록**: 지금은 k6와 서버가 같은 PC에서 돌기 때문에 서로 CPU를 나눠 씁니다. 절대값보다 **개선 전후 비교용**이라는 점을 문서에 적어두세요.
5. **이전 숫자와 섞지 않기**: 기존 JMeter 값(48,990ms, 16,450ms)은 측정 조건이 다를 수 있습니다. 이번에 잰 값을 새 기준선으로 쓰고, 이후 비교는 모두 k6 값끼리만 합니다.

---

## 9. 기록 양식

아래 표를 채워서 트러블슈팅 문서에 그대로 옮기면 됩니다.

**측정 환경**: (CPU / 메모리 / OS) · MySQL 버전 · Redis 버전 · k6 버전 · 50 VU × 5회

| 단계 | 대상 | avg | med | p95 | p99 | 실패율 | Redis 구간 | DB 구간 | 직렬화+커밋 |
|---|---|---|---|---|---|---|---|---|---|
| 개선 전 | 구역 10,000석 | | | | | | | | |
| 개선 전 | 전체 30,000석 | | | | | | | | |
| MGET 적용 | 구역 10,000석 | | | | | | | | |
| + readOnly | 구역 10,000석 | | | | | | | | |
