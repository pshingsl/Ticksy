import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { loadTossPayments } from '@tosspayments/tosspayments-sdk';
import Header from '../../components/Header';

const CLIENT_KEY = process.env.REACT_APP_TOSS_CLIENT_KEY || '';

export default function PaymentPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { paymentReady } = location.state || {};
  const initialized = useRef(false);

  useEffect(() => {
    if (!paymentReady || initialized.current) return;
    initialized.current = true;
    initToss();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const initToss = async () => {
    try {
      const tossPayments = await loadTossPayments(CLIENT_KEY);

      const payment = tossPayments.payment({
        customerKey: paymentReady.customerEmail,
      });

      await payment.requestPayment({
        method: 'CARD',
        amount: {
          currency: 'KRW',
          value: paymentReady.amount,
        },
        orderId: paymentReady.orderId,
        orderName: paymentReady.orderName,
        customerName: paymentReady.customerName,
        customerEmail: paymentReady.customerEmail,
        successUrl: `${window.location.origin}/payment/complete`,
        failUrl: `${window.location.origin}/payment/fail`,
      });
    } catch (err: any) {
      if (err.code === 'USER_CANCEL') {
        alert('결제를 취소했습니다. 선점은 유지됩니다.');
        navigate(-1);
      } else {
        alert('결제 중 오류가 발생했습니다.');
        navigate(-1);
      }
    }
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f5f5f0' }}>
      <Header />
      <div
        style={{
          textAlign: 'center',
          padding: '80px 0',
          color: '#888',
          fontSize: '14px',
        }}
      >
        결제창을 불러오는 중...
      </div>
    </div>
  );
}