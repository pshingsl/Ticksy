import { useEffect, useState, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getSeatLayout, holdSeats, cancelHold } from '../../api/seat';
import {
  SeatLayout,
  SeatItem,
  SectionWithSeats,
} from '../../types/seat';
import Header from '../../components/Header';

const MAX_SEAT = 4;
const HOLD_TTL_SECONDS = 300; // 5분

export default function SeatPage() {
  const { scheduleId } = useParams();
  const navigate = useNavigate();

  // concertId를 state로 받아옴 (이전 페이지에서 navigate 시 전달)
  const [concertId, setConcertId] = useState<number | null>(null);
  const [layout, setLayout] = useState<SeatLayout | null>(null);
  const [selectedSeatIds, setSelectedSeatIds] = useState<number[]>([]);
  const [selectedSection, setSelectedSection] =
    useState<SectionWithSeats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [holdError, setHoldError] = useState('');

  // 선점 타이머
  const [timeLeft, setTimeLeft] = useState<number | null>(null);
  const [isHeld, setIsHeld] = useState(false);

  // concertId는 URL에서 가져와야 하는데
  // 현재 라우팅이 /seats/:scheduleId 라 concertId가 없음
  // navigate state로 받거나 URL 변경 필요
  // 임시로 localStorage에서 가져오는 방식 사용
  useEffect(() => {
    const storedConcertId = localStorage.getItem('currentConcertId');
    if (storedConcertId) {
      setConcertId(Number(storedConcertId));
    } else {
      setError('공연 정보를 찾을 수 없습니다.');
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (concertId && scheduleId) {
      fetchLayout();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [concertId, scheduleId]);

  // 타이머
  useEffect(() => {
    if (!isHeld || timeLeft === null) return;

    if (timeLeft <= 0) {
      alert('선점이 만료되었습니다. 좌석을 다시 선택해주세요.');
      setIsHeld(false);
      setSelectedSeatIds([]);
      setTimeLeft(null);
      fetchLayout();
      return;
    }

    const timer = setTimeout(() => {
      setTimeLeft((prev) => (prev !== null ? prev - 1 : null));
    }, 1000);

    if (timeLeft === 60) {
      alert('선점이 곧 만료됩니다!');
    }

    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isHeld, timeLeft]);

  // 페이지 이탈 시 선점 취소
  useEffect(() => {
    return () => {
      if (isHeld && selectedSeatIds.length > 0 && scheduleId) {
        cancelHold(Number(scheduleId), selectedSeatIds).catch(() => {});
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchLayout = async () => {
    if (!concertId || !scheduleId) return;
    setLoading(true);
    setError('');
    try {
      const data = await getSeatLayout(concertId, Number(scheduleId));
      setLayout(data);
      if (data.sections.length > 0) {
        setSelectedSection(data.sections[0]);
      }
    } catch (err: any) {
      const code = err.response?.data?.code;
      if (code === 'BOOKING_NOT_OPEN_YET') {
        setError('아직 예매 오픈 전입니다.');
      } else {
        setError('좌석 정보를 불러오지 못했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  // 좌석 클릭
  const handleSeatClick = (seat: SeatItem) => {
    if (isHeld) return; // 이미 선점 완료 상태
    if (seat.status === 'RESERVED' || seat.status === 'HOLDING') return;

    setHoldError('');
    setSelectedSeatIds((prev) => {
      if (prev.includes(seat.seatId)) {
        return prev.filter((id) => id !== seat.seatId);
      }
      if (prev.length >= MAX_SEAT) {
        setHoldError(`최대 ${MAX_SEAT}석까지 선택 가능합니다.`);
        return prev;
      }
      return [...prev, seat.seatId];
    });
  };

  // 예매하기 버튼 (선점 요청)
  const handleHold = async () => {
    if (selectedSeatIds.length === 0) {
      setHoldError('좌석을 선택해주세요.');
      return;
    }
    setHoldError('');
    try {
      await holdSeats(Number(scheduleId), selectedSeatIds);
      setIsHeld(true);
      setTimeLeft(HOLD_TTL_SECONDS);

      // 예매 정보 확인 페이지로 이동
      navigate('/reservations/preview', {
        state: {
          scheduleId: Number(scheduleId),
          seatIds: selectedSeatIds,
          concertId,
        },
      });
    } catch (err: any) {
      const code = err.response?.data?.code;
      if (code === 'ALREADY_HELD_SEAT') {
        setHoldError('이미 선점된 좌석입니다. 다른 좌석을 선택해주세요.');
        fetchLayout();
      } else if (code === 'ALREADY_RESERVED_SEAT') {
        setHoldError('이미 예매 완료된 좌석입니다.');
        fetchLayout();
      } else if (code === 'EXCEED_MAX_SEAT_COUNT') {
        setHoldError('최대 4석까지 예매 가능합니다.');
      } else if (code === 'BOOKING_NOT_OPEN_YET') {
        setHoldError('예매 오픈 전입니다.');
      } else {
        setHoldError('선점 요청 중 오류가 발생했습니다.');
      }
    }
  };

  // 선택 취소
  const handleCancelSelect = (seatId: number) => {
    setSelectedSeatIds((prev) => prev.filter((id) => id !== seatId));
  };

  // 타이머 포맷
  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  // 선택한 좌석 정보 가져오기
  const getSelectedSeatInfo = useCallback(() => {
    if (!layout) return [];
    return layout.sections.flatMap((sec) =>
      sec.seats
        .filter((seat) => selectedSeatIds.includes(seat.seatId))
        .map((seat) => ({ ...seat, grade: sec.grade, price: sec.price, sectionName: sec.name }))
    );
  }, [layout, selectedSeatIds]);

  const totalPrice = getSelectedSeatInfo().reduce(
    (sum, seat) => sum + seat.price,
    0
  );

  if (loading) {
    return (
      <div style={styles.page}>
        <Header />
        <div style={styles.center}>로딩 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.page}>
        <Header />
        <div style={styles.center}>{error}</div>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <Header />
      <div style={styles.container}>

        {/* 상단 공연 정보 + 타이머 */}
        <div style={styles.topBar}>
          <div>
            <div style={styles.scheduleInfo}>
              회차 ID: {scheduleId}
            </div>
          </div>
          {isHeld && timeLeft !== null && (
            <div
              style={{
                ...styles.timer,
                color: timeLeft <= 60 ? '#e24b4a' : '#a32d2d',
                backgroundColor: timeLeft <= 60 ? '#fee' : '#fcebeb',
              }}
            >
              ⏱ {formatTime(timeLeft)} 후 선점 만료
            </div>
          )}
        </div>

        {/* 유의사항 */}
        <div style={styles.notice}>
          <strong>예매 유의사항</strong>
          <span style={styles.noticeDot}>·</span>
          선점 후 5분 이내 결제 미완료 시 자동 해제
          <span style={styles.noticeDot}>·</span>
          1인당 최대 4매
          <span style={styles.noticeDot}>·</span>
          7일 전 전액환불 / 3일 전 70% / 3일 이내 불가
        </div>

        <div style={styles.body}>
          {/* 좌측 — 좌석 배치도 */}
          <div style={styles.left}>

            {/* 구역 탭 */}
            <div style={styles.tabs}>
              {layout?.sections.map((sec) => (
                <button
                  key={sec.sectionId}
                  style={{
                    ...styles.tab,
                    ...(selectedSection?.sectionId === sec.sectionId
                      ? styles.tabActive
                      : {}),
                  }}
                  onClick={() => setSelectedSection(sec)}
                >
                  {sec.name} ({sec.price.toLocaleString()}원)
                </button>
              ))}
            </div>

            {/* 무대 */}
            <div style={styles.stage}>무대 STAGE</div>

            {/* 좌석 범례 */}
            <div style={styles.legend}>
              <div style={styles.legendItem}>
                <div style={{ ...styles.legendDot, backgroundColor: '#eaf3de', border: '1px solid #97c459' }} />
                선택가능
              </div>
              <div style={styles.legendItem}>
                <div style={{ ...styles.legendDot, backgroundColor: '#d3d1c7', border: '1px solid #888' }} />
                선점중
              </div>
              <div style={styles.legendItem}>
                <div style={{ ...styles.legendDot, backgroundColor: '#fcebeb', border: '1px solid #f09595' }} />
                예매완료
              </div>
              <div style={styles.legendItem}>
                <div style={{ ...styles.legendDot, backgroundColor: '#e8c547', border: '1px solid #ba7517' }} />
                선택됨
              </div>
            </div>

            {/* 좌석 그리드 */}
            {selectedSection && (
              <div style={styles.seatGrid}>
                {/* 열 번호 헤더 */}
                <div style={styles.seatRow}>
                  <div style={styles.rowLabel} />
                  {Array.from(
                    { length: selectedSection.seats.reduce(
                      (max, s) => Math.max(max, s.colNum), 0
                    )},
                    (_, i) => (
                      <div key={i} style={styles.colLabel}>
                        {i + 1}
                      </div>
                    )
                  )}
                </div>

                {/* 행별 좌석 */}
                {Array.from(
                  { length: selectedSection.seats.reduce(
                    (max, s) => Math.max(max, s.rowNum), 0
                  )},
                  (_, rowIdx) => {
                    const rowNum = rowIdx + 1;
                    const rowSeats = selectedSection.seats
                      .filter((s) => s.rowNum === rowNum)
                      .sort((a, b) => a.colNum - b.colNum);

                    return (
                      <div key={rowIdx} style={styles.seatRow}>
                        <div style={styles.rowLabel}>{rowNum}</div>
                        {rowSeats.map((seat) => {
                          const isSelected = selectedSeatIds.includes(seat.seatId);
                          const seatStyle = isSelected
                            ? styles.seatSelected
                            : seat.status === 'RESERVED'
                            ? styles.seatReserved
                            : seat.status === 'HOLDING'
                            ? styles.seatHolding
                            : styles.seatAvailable;

                          return (
                            <div
                              key={seat.seatId}
                              style={{ ...styles.seat, ...seatStyle }}
                              onClick={() => handleSeatClick(seat)}
                              title={`${rowNum}행 ${seat.colNum}열`}
                            />
                          );
                        })}
                      </div>
                    );
                  }
                )}
              </div>
            )}
          </div>

          {/* 우측 — 선택 좌석 + 결제 */}
          <div style={styles.right}>
            <div style={styles.summaryCard}>
              <div style={styles.summaryTitle}>선택한 좌석</div>

              {selectedSeatIds.length === 0 ? (
                <div style={styles.emptySeat}>
                  좌석을 선택해주세요.
                </div>
              ) : (
                getSelectedSeatInfo().map((seat) => (
                  <div key={seat.seatId} style={styles.selectedSeatRow}>
                    <span style={styles.selectedSeatInfo}>
                      {seat.sectionName} {seat.rowNum}행 {seat.colNum}열
                    </span>
                    <div style={styles.selectedSeatRight}>
                      <span style={styles.selectedSeatPrice}>
                        {seat.price.toLocaleString()}원
                      </span>
                      {!isHeld && (
                        <button
                          style={styles.removeBtn}
                          onClick={() => handleCancelSelect(seat.seatId)}
                        >
                          ✕
                        </button>
                      )}
                    </div>
                  </div>
                ))
              )}

              {selectedSeatIds.length > 0 && (
                <div style={styles.totalRow}>
                  <span>합계</span>
                  <span style={styles.totalPrice}>
                    {totalPrice.toLocaleString()}원
                  </span>
                </div>
              )}

              {holdError && (
                <p style={styles.error}>{holdError}</p>
              )}

              {isHeld && timeLeft !== null && (
                <div style={styles.timerBox}>
                  ⏱ {formatTime(timeLeft)} 후 선점 만료
                </div>
              )}

              <button
                style={{
                  ...styles.holdBtn,
                  opacity: selectedSeatIds.length === 0 || isHeld ? 0.5 : 1,
                  cursor:
                    selectedSeatIds.length === 0 || isHeld
                      ? 'not-allowed'
                      : 'pointer',
                }}
                onClick={handleHold}
                disabled={selectedSeatIds.length === 0 || isHeld}
              >
                예매하기
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: { minHeight: '100vh', backgroundColor: '#f5f5f0' },
  container: { maxWidth: '1200px', margin: '0 auto', padding: '24px 40px' },
  center: { textAlign: 'center', padding: '80px 0', color: '#888' },
  topBar: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '16px',
  },
  scheduleInfo: { fontSize: '15px', fontWeight: '600', color: '#1a1a1a' },
  timer: {
    padding: '6px 14px',
    borderRadius: '6px',
    fontSize: '13px',
    fontWeight: '700',
  },
  notice: {
    backgroundColor: '#faeeda',
    borderRadius: '8px',
    padding: '10px 16px',
    fontSize: '12px',
    color: '#854f0b',
    marginBottom: '20px',
    display: 'flex',
    gap: '8px',
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  noticeDot: { color: '#c8a050' },
  body: { display: 'flex', gap: '24px', alignItems: 'flex-start' },
  left: { flex: 1 },
  right: { width: '260px', flexShrink: 0 },
  tabs: { display: 'flex', gap: '8px', marginBottom: '16px', flexWrap: 'wrap' },
  tab: {
    padding: '7px 14px',
    border: '1px solid #ddd',
    borderRadius: '6px',
    backgroundColor: '#fff',
    fontSize: '13px',
    cursor: 'pointer',
  },
  tabActive: {
    backgroundColor: '#1a1a1a',
    color: '#fff',
    borderColor: '#1a1a1a',
  },
  stage: {
    backgroundColor: '#333',
    color: '#fff',
    textAlign: 'center',
    padding: '8px',
    borderRadius: '6px',
    fontSize: '12px',
    marginBottom: '16px',
    maxWidth: '400px',
  },
  legend: {
    display: 'flex',
    gap: '16px',
    marginBottom: '16px',
    flexWrap: 'wrap',
  },
  legendItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    fontSize: '11px',
    color: '#555',
  },
  legendDot: { width: '14px', height: '14px', borderRadius: '3px' },
  seatGrid: { display: 'flex', flexDirection: 'column', gap: '3px' },
  seatRow: { display: 'flex', gap: '3px', alignItems: 'center' },
  rowLabel: {
    width: '20px',
    fontSize: '10px',
    color: '#888',
    textAlign: 'center',
    flexShrink: 0,
  },
  colLabel: {
    width: '24px',
    height: '16px',
    fontSize: '9px',
    color: '#aaa',
    textAlign: 'center',
    flexShrink: 0,
  },
  seat: {
    width: '24px',
    height: '24px',
    borderRadius: '4px',
    cursor: 'pointer',
    flexShrink: 0,
    transition: 'opacity 0.1s',
  },
  seatAvailable: {
    backgroundColor: '#eaf3de',
    border: '1px solid #97c459',
  },
  seatHolding: {
    backgroundColor: '#d3d1c7',
    border: '1px solid #888',
    cursor: 'not-allowed',
  },
  seatReserved: {
    backgroundColor: '#fcebeb',
    border: '1px solid #f09595',
    cursor: 'not-allowed',
  },
  seatSelected: {
    backgroundColor: '#e8c547',
    border: '1px solid #ba7517',
  },
  summaryCard: {
    backgroundColor: '#fff',
    border: '1px solid #e0e0e0',
    borderRadius: '10px',
    padding: '20px',
  },
  summaryTitle: {
    fontSize: '14px',
    fontWeight: '700',
    marginBottom: '12px',
    color: '#1a1a1a',
  },
  emptySeat: {
    fontSize: '13px',
    color: '#aaa',
    textAlign: 'center',
    padding: '20px 0',
  },
  selectedSeatRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '8px',
    backgroundColor: '#f5f5f0',
    borderRadius: '6px',
    marginBottom: '6px',
  },
  selectedSeatInfo: { fontSize: '12px', color: '#333' },
  selectedSeatRight: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
  },
  selectedSeatPrice: { fontSize: '12px', fontWeight: '600' },
  removeBtn: {
    background: 'none',
    border: 'none',
    color: '#aaa',
    cursor: 'pointer',
    fontSize: '11px',
    padding: '0 2px',
  },
  totalRow: {
    display: 'flex',
    justifyContent: 'space-between',
    borderTop: '1px solid #e0e0e0',
    paddingTop: '10px',
    marginTop: '6px',
    fontSize: '13px',
  },
  totalPrice: { fontWeight: '700', fontSize: '15px' },
  error: { color: '#e24b4a', fontSize: '12px', marginTop: '8px' },
  timerBox: {
    backgroundColor: '#fcebeb',
    color: '#a32d2d',
    borderRadius: '6px',
    padding: '8px',
    fontSize: '13px',
    fontWeight: '600',
    textAlign: 'center',
    marginTop: '8px',
  },
  holdBtn: {
    width: '100%',
    padding: '12px',
    backgroundColor: '#e8c547',
    color: '#1a1a1a',
    border: 'none',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '700',
    marginTop: '12px',
  },
};