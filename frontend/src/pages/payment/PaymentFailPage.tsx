import { useNavigate, useSearchParams } from 'react-router-dom';
import Header from '../../components/Header';

export default function PaymentFailPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const errorCode = searchParams.get('code');
  const errorMessage = searchParams.get('message');

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f5f5f0' }}>
      <Header />
      <div
        style={{
          maxWidth: '480px',
          margin: '0 auto',
          padding: '48px 24px',
          textAlign: 'center',
        }}
      >
        <div
          style={{
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
          }}
        >
          ✕
        </div>
        <div
          style={{
            fontSize: '20px',
            fontWeight: '700',
            marginBottom: '8px',
          }}
        >
          결제에 실패했습니다
        </div>
        <div
          style={{
            fontSize: '13px',
            color: '#888',
            marginBottom: '8px',
          }}
        >
          {errorMessage || '결제 처리 중 오류가 발생했습니다.'}
        </div>
        {errorCode && (
          <div
            style={{
              fontSize: '12px',
              color: '#bbb',
              marginBottom: '24px',
            }}
          >
            에러 코드: {errorCode}
          </div>
        )}
        <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
          <button
            style={{
              padding: '12px 24px',
              border: '1px solid #ddd',
              borderRadius: '8px',
              backgroundColor: '#fff',
              cursor: 'pointer',
              fontSize: '14px',
            }}
            onClick={() => navigate('/')}
          >
            메인으로
          </button>
          <button
            style={{
              padding: '12px 24px',
              border: 'none',
              borderRadius: '8px',
              backgroundColor: '#1a1a1a',
              color: '#fff',
              cursor: 'pointer',
              fontSize: '14px',
            }}
            onClick={() => navigate(-2)}
          >
            다시 시도
          </button>
        </div>
      </div>
    </div>
  );
}