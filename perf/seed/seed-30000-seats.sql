-- =====================================================================
-- Ticksy 성능 측정용 시드 데이터
--   - 측정 전용 회차 1개 + 구역 3개(각 100행 x 100열 = 10,000석) = 총 30,000석
--   - 측정 전용 계정 1개 (perf@ticksy.com / Perf1234!)
--
-- 실행 시점: 애플리케이션을 한 번 띄워서 테이블이 생성된 뒤 실행
-- 다시 실행해도 됨: 기존 측정 데이터를 지우고 새로 만듦
-- =====================================================================

SET SQL_SAFE_UPDATES = 0;   -- MySQL Workbench 안전 모드 해제 (DELETE 허용)

-- 0. 이전 측정 데이터 정리 ------------------------------------------------
SET @old_schedule_id := (
    SELECT schedule_id FROM event_schedules
    WHERE concert_id = 1 AND event_date = '2027-01-01' AND event_time = '18:00:00'
);

DELETE st FROM seats st
JOIN sections sc ON st.section_id = sc.section_id
WHERE sc.schedule_id = @old_schedule_id;

DELETE FROM sections WHERE schedule_id = @old_schedule_id;
DELETE FROM event_schedules WHERE schedule_id = @old_schedule_id;

-- 1. 측정용 계정 -----------------------------------------------------------
-- 비밀번호 Perf1234! 의 BCrypt 해시 (회원가입은 이메일 인증이 필요해서 직접 넣음)
INSERT IGNORE INTO users (email, password, name, role, is_deleted, created_at)
VALUES ('perf@ticksy.com',
        '$2a$10$hLRQ0DAsqZCj60lhrzXPoepGurBGXMb1qxPtoXWIJrdvlIE96hkoa',
        '성능측정', 'USER', 0, NOW());

-- 2. 측정용 회차 (concert_id = 1, 예매는 이미 열린 상태) ---------------------
INSERT INTO event_schedules (concert_id, event_date, event_time, booking_open_at, status, created_at)
VALUES (1, '2027-01-01', '18:00:00', '2026-01-01 10:00:00', 'OPEN', NOW());

SET @schedule_id := LAST_INSERT_ID();

-- 3. 구역 3개 (각 100 x 100) -----------------------------------------------
INSERT INTO sections (schedule_id, name, grade, price, row_count, col_count, created_at) VALUES
(@schedule_id, 'A구역', 'VIP', 5000, 100, 100, NOW()),
(@schedule_id, 'B구역', 'R',   2000, 100, 100, NOW()),
(@schedule_id, 'C구역', 'S',    900, 100, 100, NOW());

-- 4. 좌석 30,000개 (1~100 숫자를 두 자리 조합으로 생성) ----------------------
INSERT INTO seats (section_id, row_num, col_num, status, created_at)
SELECT sc.section_id, r.n, c.n, 'AVAILABLE', NOW()
FROM sections sc
JOIN (
    SELECT a.d * 10 + b.d + 1 AS n
    FROM (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
    CROSS JOIN
         (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
) r
JOIN (
    SELECT a.d * 10 + b.d + 1 AS n
    FROM (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
    CROSS JOIN
         (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
) c
WHERE sc.schedule_id = @schedule_id;

SET SQL_SAFE_UPDATES = 1;

-- 5. k6에 넣을 값 확인 ------------------------------------------------------
--    아래 결과의 schedule_id, section_id를 k6 실행 명령에 그대로 사용
SELECT sc.schedule_id, sc.section_id, sc.name, COUNT(st.seat_id) AS seat_count
FROM sections sc
JOIN seats st ON st.section_id = sc.section_id
WHERE sc.schedule_id = @schedule_id
GROUP BY sc.schedule_id, sc.section_id, sc.name;
