import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Header from '../../components/Header';
import { confirmPayment } from '../../api/reservation';
import { PaymentConfirmResult } from '../../types/reservation';

export default function PaymentCompletePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [result, setResult] = useState<PaymentConfirmResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const paymentKey = searchParams.get('paymentKey');
    const orderId = searchParams.get('orderId');
    const amount = searchParams.get('amount');

    if (!paymentKey || !orderId || !amount) {
      setError('결제 정보가 올바르지 않습니다.');
      setLoading(false);
      return;
    }

    handleConfirm(paymentKey, orderId, Number(amount));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleConfirm = async (
    paymentKey: string,
    orderId: string,
    amount: number
  ) => {
    setLoading(true);
    try {
      const data = await confirmPayment(paymentKey, orderId, amount);
      setResult(data);
    } catch (err: any) {
      const code = err.response?.data?.code;
      if (code === 'SEAT_HOLD_EXPIRED') {
        setError('좌석 선점이 만료되었습니다. 처음부터 다시 시도해주세요.');
      } else if (code === 'PAYMENT_CONFIRM_FAILED') {
        setError('결제 승인에 실패했습니다. 고객센터에 문의해주세요.');
      } else {
        setError('결제 처리 중 오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={styles.page}>
        <Header />
        <div style={styles.center}>결제를 처리하는 중입니다...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.page}>
        <Header />
        <div style={styles.center}>
          <div style={styles.errorIcon}>✕</div>
          <p style={{ color: '#e24b4a', marginBottom: '20px' }}>{error}</p>
          <button style={styles.btn} onClick={() => navigate('/')}>
            메인으로
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <Header />
      <div style={styles.container}>
        <div style={styles.card}>
          {/* 완료 아이콘 */}
          <div style={styles.successIcon}>✓</div>
          <div style={styles.title}>예매가 완료되었습니다!</div>
          <div style={styles.subtitle}>
            아래 예매 정보를 확인해주세요.
          </div>

          {/* 예매 정보 */}
          <div style={styles.infoBox}>
            <div style={styles.infoRow}>
              <span style={styles.label}>예매번호</span>
              <span style={styles.reservationCode}>
                {result?.reservationCode}
              </span>
            </div>
            <div style={styles.infoRow}>
              <span style={styles.label}>결제일시</span>
              <span style={styles.value}>
                {result?.paidAt
                  ? new Date(result.paidAt).toLocaleString('ko-KR')
                  : '-'}
              </span>
            </div>
          </div>

          {/* 버튼 */}
          <div style={styles.btnRow}>
            <button
              style={styles.btnSecondary}
              onClick={() => navigate('/')}
            >
              메인으로
            </button>
            <button
              style={styles.btnPrimary}
              onClick={() => navigate('/mypage')}
            >
              마이페이지
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: { minHeight: '100vh', backgroundColor: '#f5f5f0' },
  container: { maxWidth: '480px', margin: '0 auto', padding: '48px 24px' },
  center: {
    textAlign: 'center',
    padding: '80px 24px',
    color: '#888',
    fontSize: '14px',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '40px',
    textAlign: 'center',
    boxShadow: '0 2px 16px rgba(0,0,0,0.08)',
  },
  successIcon: {
    width: '64px',
    height: '64px',
    borderRadius: '50%',
    backgroundColor: '#eaf3de',
    color: '#3b7d22',
    fontSize: '28px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 16px',
  },
  errorIcon: {
    width: '64px',
    height: '64px',
    borderRadius: '50%',
    backgroundColor: '#fcebeb',
    color: '#e24b4a',
    fontSize: '28px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 16px',
  },
  title: {
    fontSize: '20px',
    fontWeight: '700',
    color: '#1a1a1a',
    marginBottom: '8px',
  },
  subtitle: {
    fontSize: '13px',
    color: '#888',
    marginBottom: '24px',
  },
  infoBox: {
    backgroundColor: '#f5f5f0',
    borderRadius: '8px',
    padding: '16px',
    marginBottom: '24px',
    textAlign: 'left',
  },
  infoRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
    fontSize: '13px',
  },
  label: { color: '#888' },
  reservationCode: {
    fontWeight: '700',
    fontSize: '14px',
    color: '#1a1a1a',
    fontFamily: 'monospace',
  },
  value: { color: '#1a1a1a' },
  btnRow: { display: 'flex', gap: '8px' },
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
    padding: '12px',
    border: '1px solid #ddd',
    borderRadius: '8px',
    backgroundColor: '#fff',
    cursor: 'pointer',
    fontSize: '14px',
  },
  btnPrimary: {
    flex: 1,
    padding: '12px',
    border: 'none',
    borderRadius: '8px',
    backgroundColor: '#1a1a1a',
    color: '#fff',
    fontWeight: '600',
    fontSize: '14px',
    cursor: 'pointer',
  },
};