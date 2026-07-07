import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../../components/Header';
import { getReservationPreview, requestPayment } from '../../api/reservation';
import { ReservationPreview } from '../../types/reservation';

const HOLD_TTL_SECONDS = 300;

export default function ReservationPreivewPage() {
  const navigate = useNavigate();
  const location = useLocation();

  // SeatPage에서  navigate시 state로 전달
  const { scheduleId, seatIds, concertId } = location.state || {};

  const [preview, setPreview] = useState<ReservationPreview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [timeLeft, setTimeLeft] = useState(HOLD_TTL_SECONDS);
  const [paying, setPaying] = useState(false);

  useEffect(() => {
    if (!scheduleId || !seatIds) {
      setError('잘못된 접근입니다.');
      setLoading(false);
      return;
    }
    fetchPreview();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 카운트다운 타이머
  useEffect(() => {
    if (timeLeft <= 0) {
      alert('선점이 만료되었습니다. 좌석을 다시 선택해주세요.');
      navigate(-1);
      return;
    }

    const timer = setTimeout(() => setTimeLeft((t) => t - 1), 1000);

    if (timeLeft === 60) {
      alert('선점이 곧 만료됩니다!');
    }

    return () => clearTimeout(timer);
  }, [timeLeft, navigate]);

  const fetchPreview = async () => {
    setLoading(true);

    try {
      const data = await getReservationPreview(scheduleId, seatIds);
      setPreview(data);

      // holdExpiredAt 기준으로 남은 시간 계산
      if (data.holdExpiredAt) {
        const expiredAt = new Date(data.holdExpiredAt).getTime();
        const now = new Date().getTime();
        const remainSeconds = Math.floor((expiredAt - now) / 1000);
        if (remainSeconds > 0) {
          setTimeLeft(remainSeconds);
        }
      }
    } catch (err: any) {
      const code = err.response?.data.code;
      if (code === 'SEAT_NOT_HOLD' || code === 'SEAT_HOLD_EXPIRED') {
        setError('좌석 선점이 만료되었습니다. 좌석을 다시 선택해주세요.');
      } else {
        setError('예매 정보를 불러오지 못했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  const handlePayment = async () => {
    if (!preview) return;
    setPaying(true);

    try {
      const paymentReady = await requestPayment(
        scheduleId,
        seatIds,
        preview.totalPrice
      );

      // Toss 결제 페이지로 이동(state 데이터 전달)
      navigate('/payment', {
        state: {
          paymentReady,
          scheduleId,
          seatIds,
        },
      });
    } catch (err: any) {
      const code = err.response?.data?.code;
      if (code === 'SEAT_HOLD_EXPIRED') {
        alert('좌석 선점이 만료되었습니다. 좌석을 다시 선택해주세요.');
        navigate(-2);
      } else if (code === 'PRICE_MATCH') {
        alert('결제 금액 오류가 발생했습니다.');
      } else {
        alert('결제 요청 중 오류가 발생했습니다.');
      }
      setPaying(false);
    }
  };

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
        <div style={styles.center}>
          <p style={{ color: '#e24b4a', marginBottom: '16px' }}>{error}</p>
          <button style={styles.btn} onClick={() => navigate(-1)}>
            돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <Header />
      <div style={styles.container}>
        {/* 단계 표시 */}
        <div style={styles.steps}>
          <div style={styles.stepDone}>✓ 좌석 선택</div>
          <div style={styles.stepLine} />
          <div style={styles.stepActive}>2 예매 확인</div>
          <div style={styles.stepLine} />
          <div style={styles.stepInactive}>3 결제</div>
          <div style={styles.stepLine} />
          <div style={styles.stepInactive}>4 완료</div>
        </div>

        <div style={styles.topBar}>
          <div style={styles.title}>예매 정보 확인</div>
          <div
            style={{
              ...styles.timer,
              backgroundColor: timeLeft <= 60 ? '#fee' : '#fcebeb',
              color: timeLeft <= 60 ? '#c0392b' : '#a32d2d',
            }}
          >
            ⏱ {formatTime(timeLeft)} 후 선점 만료
          </div>
        </div>

        {/* 공연 정보 */}
        <div style={styles.card}>
          <div style={styles.cardTitle}>공연 정보</div>
          <div style={styles.infoRow}>
            <span style={styles.label}>공연명</span>
            <span style={styles.value}>{preview?.concertTitle}</span>
          </div>
          <div style={styles.infoRow}>
            <span style={styles.label}>공연장</span>
            <span style={styles.value}>{preview?.venueName}</span>
          </div>
          <div style={styles.infoRow}>
            <span style={styles.label}>일시</span>
            <span style={styles.value}>
              {preview?.eventDate} {preview?.eventTime?.slice(0, 5)}
            </span>
          </div>
        </div>

        {/* 선택 좌석 */}
        <div style={styles.card}>
          <div style={styles.cardTitle}>선택 좌석</div>
          {preview?.seats.map((seat) => (
            <div key={seat.seatId} style={styles.seatRow}>
              <span style={styles.seatInfo}>
                {seat.sectionName} {seat.rowNum}행 {seat.colNum}열 ({seat.grade})
              </span>
              <span style={styles.seatPrice}>
                {seat.price.toLocaleString()}원
              </span>
            </div>
          ))}
        </div>

        {/* 총 금액 */}
        <div style={styles.totalBox}>
          <span style={styles.totalLabel}>총 결제 금액</span>
          <span style={styles.totalPrice}>
            {preview?.totalPrice.toLocaleString()}원
          </span>
        </div>

        {/* 버튼 */}
        <div style={styles.btnRow}>
          <button
            style={styles.btnSecondary}
            onClick={() => navigate(-1)}
            disabled={paying}
          >
            이전으로
          </button>
          <button
            style={{
              ...styles.btnPrimary,
              opacity: paying ? 0.7 : 1,
              cursor: paying ? 'not-allowed' : 'pointer',
            }}
            onClick={handlePayment}
            disabled={paying}
          >
            {paying ? '처리 중...' : '결제하기'}
          </button>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: { minHeight: '100vh', backgroundColor: '#f5f5f0' },
  container: { maxWidth: '640px', margin: '0 auto', padding: '32px 24px' },
  center: { textAlign: 'center', padding: '80px 0', color: '#888' },
  steps: {
    display: 'flex',
    alignItems: 'center',
    marginBottom: '32px',
    fontSize: '13px',
  },
  stepDone: {
    color: '#e8c547',
    fontWeight: '600',
    whiteSpace: 'nowrap',
  },
  stepActive: {
    color: '#1a1a1a',
    fontWeight: '700',
    whiteSpace: 'nowrap',
  },
  stepInactive: { color: '#aaa', whiteSpace: 'nowrap' },
  stepLine: {
    flex: 1,
    height: '1px',
    backgroundColor: '#ddd',
    margin: '0 8px',
  },
  topBar: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
  },
  title: { fontSize: '20px', fontWeight: '700', color: '#1a1a1a' },
  timer: {
    padding: '6px 14px',
    borderRadius: '6px',
    fontSize: '13px',
    fontWeight: '700',
  },
  card: {
    backgroundColor: '#fff',
    border: '1px solid #e0e0e0',
    borderRadius: '10px',
    padding: '20px',
    marginBottom: '16px',
  },
  cardTitle: {
    fontSize: '14px',
    fontWeight: '700',
    color: '#1a1a1a',
    marginBottom: '14px',
    paddingBottom: '10px',
    borderBottom: '1px solid #f0f0f0',
  },
  infoRow: {
    display: 'flex',
    justifyContent: 'space-between',
    marginBottom: '8px',
    fontSize: '13px',
  },
  label: { color: '#888' },
  value: { color: '#1a1a1a', fontWeight: '500' },
  seatRow: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '8px',
    backgroundColor: '#f5f5f0',
    borderRadius: '6px',
    marginBottom: '6px',
    fontSize: '13px',
  },
  seatInfo: { color: '#333' },
  seatPrice: { fontWeight: '600', color: '#1a1a1a' },
  totalBox: {
    backgroundColor: '#fff',
    border: '1px solid #e0e0e0',
    borderRadius: '10px',
    padding: '16px 20px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
  },
  totalLabel: { fontSize: '14px', fontWeight: '600', color: '#1a1a1a' },
  totalPrice: {
    fontSize: '20px',
    fontWeight: '700',
    color: '#1a1a1a',
  },
  btnRow: { display: 'flex', gap: '10px' },
  btn: {
    padding: '10px 20px',
    border: '1px solid #ddd',
    borderRadius: '8px',
    backgroundColor: '#fff',
    cursor: 'pointer',
    fontSize: '14px',
  },
  btnSecondary: {
    flex: 1,
    padding: '14px',
    border: '1px solid #ddd',
    borderRadius: '8px',
    backgroundColor: '#fff',
    cursor: 'pointer',
    fontSize: '14px',
  },
  btnPrimary: {
    flex: 2,
    padding: '14px',
    border: 'none',
    borderRadius: '8px',
    backgroundColor: '#e8c547',
    color: '#1a1a1a',
    fontWeight: '700',
    fontSize: '14px',
  },
};