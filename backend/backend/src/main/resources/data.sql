-- Ticksy 샘플 데이터

-- 관리자 계정 (비밀번호: admin123$)
INSERT INTO users (email, password, name, role, is_deleted, created_at)
VALUES ('admin@ticksy.com', 'admin123$', '관리자', 'ADMIN', 0, NOW());

-- 일반 테스트 계정 (비밀번호: test1234!)
INSERT INTO users (email, password, name, role, is_deleted, created_at)
VALUES ('test@ticksy.com', 'test1234!', '테스트유저', 'USER', 0, NOW());

-- 공연장 (2개)
INSERT INTO venues (name, address, created_at) VALUES
('올림픽 공원 체조경기장', '서울시 송파구 올림픽로 424', NOW()),
('KSPO DOME', '서울시 송파구 올림픽로 424', NOW());

-- 공연 (2개)
INSERT INTO concerts (venue_id, title, cast, age_limit, run_time, description, poster_url, is_deleted, created_at) VALUES
(1, 'BTS World Tour 2026',
 'BTS (방탄소년단)',
 '전체관람가', 180,
 'BTS 월드투어 서울 앙코르 공연. 전 세계를 뜨겁게 달군 BTS가 드디어 서울로 돌아옵니다.',
 'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=800',
 0, NOW()),

(2, 'IU 콘서트 : The Golden Hour',
 'IU (이지은)',
 '전체관람가', 150,
 'IU의 단독 콘서트. 데뷔 15주년을 기념하는 특별한 무대로 찾아옵니다.',
 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=800',
 0, NOW());


-- BTS 회차
INSERT INTO event_schedules (concert_id, event_date, event_time, booking_open_at, status, created_at) VALUES
(1, '2026-12-25', '18:00:00', '2026-01-01 10:00:00', 'OPEN', NOW()),
(1, '2026-12-26', '18:00:00', '2026-01-01 10:00:00', 'OPEN', NOW());

-- IU 회차
INSERT INTO event_schedules (concert_id, event_date, event_time, booking_open_at, status, created_at) VALUES
(2, '2026-11-15', '19:00:00', '2026-01-01 10:00:00', 'OPEN', NOW()),
(2, '2026-11-16', '19:00:00', '2026-01-01 10:00:00', 'OPEN', NOW());


-- 구역 (회차당 3구역)
INSERT INTO sections (schedule_id, name, grade, price, row_count, col_count, created_at) VALUES
-- BTS 1회차
(1, 'A구역', 'VIP', 5000, 5, 6, NOW()),
(1, 'B구역', 'R',   2000, 5, 6, NOW()),
(1, 'C구역', 'S',    900, 5, 6, NOW()),
-- BTS 2회차
(2, 'A구역', 'VIP', 5000, 5, 6, NOW()),
(2, 'B구역', 'R',   2000, 5, 6, NOW()),
(2, 'C구역', 'S',    900, 5, 6, NOW()),
-- IU 1회차
(3, 'A구역', 'VIP', 5000, 5, 6, NOW()),
(3, 'B구역', 'R',   2000, 5, 6, NOW()),
(3, 'C구역', 'S',    900, 5, 6, NOW()),
-- IU 2회차
(4, 'A구역', 'VIP', 5000, 5, 6, NOW()),
(4, 'B구역', 'R',   2000, 5, 6, NOW()),
(4, 'C구역', 'S',    900, 5, 6, NOW());

-- 좌석 생성 (구역당 5행 × 6열 = 30석)
INSERT INTO seats (section_id, row_num, col_num, status, created_at)
SELECT s.section_id, r.row_num, c.col_num, 'AVAILABLE', NOW()
FROM sections s
JOIN (
    SELECT 1 AS row_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
) r
JOIN (
    SELECT 1 AS col_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
) c
ORDER BY s.section_id, r.row_num, c.col_num;