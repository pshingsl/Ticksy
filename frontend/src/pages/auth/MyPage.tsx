import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { changePassword, withdraw, logout } from '../../api/auth';
import { getMyReservations, cancelReservation } from '../../api/reservation';
import { useAuthStore } from '../../store/authStore';
import { ReservationListItem } from '../../types/reservation';

export default function MyPage() {
  const navigate = useNavigate();
  const { logout: storeLogout, userName } = useAuthStore();

  // 비밀번호 변경
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [pwError, setPwError] = useState('');
  const [pwSuccess, setPwSuccess] = useState('');
  const [pwLoading, setPwLoading] = useState(false);

  // 예매 내역
  const [reservations, setReservations] = useState<ReservationListItem[]>([]);
  const [reservationLoading, setReservationLoading] = useState(true);

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchReservations();
  }, []);

  // 예매 내역 조회
  const fetchReservations = async () => {
    setReservationLoading(true);
    try {
      const data = await getMyReservations();
      setReservations(data);
    } catch {
      console.error('예매 내역 조회 실패');
    } finally {
      setReservationLoading(false);
    }
  };

  // 로그아웃
  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      storeLogout();
      navigate('/login');
    }
  };

  // 비밀번호 변경
  const handleChangePassword = async () => {
    if (!currentPassword || !newPassword) {
      setPwError('모든 항목을 입력해주세요.');
      return;
    }
    if (newPassword !== newPasswordConfirm) {
      setPwError('새 비밀번호가 일치하지 않습니다.');
      return;
    }
    const pwRegex =
      /^(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/;
    if (!pwRegex.test(newPassword)) {
      setPwError('비밀번호는 8자 이상이며 특수문자를 포함해야 합니다.');
      return;
    }

    setPwLoading(true);
    setPwError('');
    setPwSuccess('');

    try {
      await changePassword(currentPassword, newPassword);
      setPwSuccess('비밀번호가 변경되었습니다. 다시 로그인해주세요.');
      setTimeout(() => {
        storeLogout();
        navigate('/login');
      }, 2000);
    } catch (err: any) {
      const code = err.response?.data?.code;
      if (code === 'INVALID_CURRENT_PASSWORD') {
        setPwError('현재 비밀번호가 일치하지 않습니다.');
      } else {
        setPwError('비밀번호 변경 중 오류가 발생했습니다.');
      }
    } finally {
      setPwLoading(false);
    }
  };

  // 예매 취소
  const handleCancelReservation = async (reservationId: number) => {
    const confirmed = window.confirm('예매를 취소하시겠습니까?');
    if (!confirmed) return;

    try {
      const result = await cancelReservation(reservationId);
      alert(
        `환불이 완료되었습니다.\n환불 금액: ${result.refundAmount.toLocaleString()}원`
      );
      fetchReservations();
    } catch (err: any) {
      const code = err.response?.data?.code;
      if (code === 'CANCEL_PERIOD_EXPIRED') {
        alert('취소 가능 기간이 지났습니다. (공연 3일 이내 취소 불가)');
      } else if (code === 'ALREADY_CANCELLED') {
        alert('이미 취소된 예매입니다.');
      } else {
        alert('취소 처리 중 오류가 발생했습니다.');
      }
    }
  };

  // 회원 탈퇴
  const handleWithdraw = async () => {
    const confirmed = window.confirm('정말 탈퇴하시겠습니까?');
    if (!confirmed) return;

    setLoading(true);
    try {
      await withdraw();
      alert('탈퇴가 완료되었습니다.');
      storeLogout();
      navigate('/login');
    } catch (err: any) {
      const code = err.response?.data?.code;
      if (code === 'HAS_ACTIVE_RESERVATION') {
        alert(
          '예매 내역이 있어 탈퇴가 불가능합니다.\n예매를 먼저 취소해주세요.'
        );
      } else {
        alert('탈퇴 처리 중 오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      {/* 헤더 */}
      <div style={styles.header}>
        <div style={styles.logo}>
          Tick<span style={styles.logoAccent}>sy</span>
        </div>
        <div style={styles.headerRight}>
          <button style={styles.homeBtn} onClick={() => navigate('/')}>
            메인으로
          </button>
          <button style={styles.logoutBtn} onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      </div>

      <div style={styles.inner}>
        {/* 사용자 정보 */}
        <div style={styles.profileCard}>
          <div style={styles.avatar}>
            {userName?.charAt(0).toUpperCase() ?? 'U'}
          </div>
          <div>
            <div style={styles.userName}>{userName}</div>
            <div style={styles.userTag}>일반 회원</div>
          </div>
        </div>

        {/* 예매 내역 */}
        <div style={styles.section}>
          <h3 style={styles.sectionTitle}>예매 내역</h3>
          {reservationLoading ? (
            <div style={styles.emptyText}>로딩 중...</div>
          ) : reservations.length === 0 ? (
            <div style={styles.emptyText}>예매 내역이 없습니다.</div>
          ) : (
            reservations.map((r) => (
              <div key={r.reservationId} style={resStyles.card}>
                <div style={resStyles.top}>
                  <span
                    style={{
                      ...resStyles.badge,
                      backgroundColor:
                        r.status === 'CONFIRMED' ? '#eaf3de' : '#fcebeb',
                      color:
                        r.status === 'CONFIRMED' ? '#3b7d22' : '#a32d2d',
                    }}
                  >
                    {r.status === 'CONFIRMED' ? '예매확정' : '취소완료'}
                  </span>
                  <span style={resStyles.code}>{r.reservationCode}</span>
                </div>
                <div style={resStyles.concertTitle}>{r.concertTitle}</div>
                <div style={resStyles.info}>
                  {r.eventDate} {r.eventTime?.slice(0, 5)} |{' '}
                  {r.venueName}
                </div>
                <div style={resStyles.bottom}>
                  <span style={resStyles.price}>
                    {r.totalPrice.toLocaleString()}원
                  </span>
                  {r.status === 'CONFIRMED' && (
                    <button
                      style={resStyles.cancelBtn}
                      onClick={() =>
                        handleCancelReservation(r.reservationId)
                      }
                    >
                      취소하기
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        {/* 비밀번호 변경 */}
        <div style={styles.section}>
          <h3 style={styles.sectionTitle}>비밀번호 변경</h3>
          <div style={styles.formGroup}>
            <label style={styles.label}>현재 비밀번호</label>
            <input
              style={styles.input}
              type="password"
              placeholder="현재 비밀번호 입력"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
            />
          </div>
          <div style={styles.formGroup}>
            <label style={styles.label}>새 비밀번호</label>
            <input
              style={styles.input}
              type="password"
              placeholder="8자 이상, 특수문자 포함"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
          </div>
          <div style={styles.formGroup}>
            <label style={styles.label}>새 비밀번호 확인</label>
            <input
              style={styles.input}
              type="password"
              placeholder="새 비밀번호 재입력"
              value={newPasswordConfirm}
              onChange={(e) => setNewPasswordConfirm(e.target.value)}
            />
          </div>

          {pwError && <p style={styles.error}>{pwError}</p>}
          {pwSuccess && <p style={styles.success}>{pwSuccess}</p>}

          <button
            style={{
              ...styles.button,
              opacity: pwLoading ? 0.7 : 1,
            }}
            onClick={handleChangePassword}
            disabled={pwLoading}
          >
            {pwLoading ? '변경 중...' : '비밀번호 변경'}
          </button>
          <p style={styles.hint}>
            ※ 변경 후 보안을 위해 자동 로그아웃됩니다.
          </p>
        </div>

        {/* 회원 탈퇴 */}
        <div style={styles.section}>
          <h3 style={styles.sectionTitle}>회원 탈퇴</h3>
          <p style={styles.hint}>
            탈퇴 시 계정 복구가 불가능합니다. 확정된 예매 내역이 있으면
            탈퇴할 수 없습니다.
          </p>
          <button
            style={styles.withdrawBtn}
            onClick={handleWithdraw}
            disabled={loading}
          >
            회원 탈퇴
          </button>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    minHeight: '100vh',
    backgroundColor: '#f5f5f0',
  },
  header: {
    height: '60px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 40px',
    backgroundColor: '#fff',
    borderBottom: '1px solid #e0e0e0',
    position: 'sticky',
    top: 0,
    zIndex: 100,
  },
  logo: {
    fontSize: '20px',
    fontWeight: '800',
    color: '#1a1a1a',
  },
  logoAccent: { color: '#e8c547' },
  headerRight: {
    display: 'flex',
    gap: '8px',
  },
  homeBtn: {
    padding: '7px 16px',
    borderRadius: '6px',
    fontSize: '13px',
    border: '1px solid #ddd',
    backgroundColor: '#fff',
    color: '#333',
    cursor: 'pointer',
  },
  logoutBtn: {
    padding: '7px 16px',
    borderRadius: '6px',
    fontSize: '13px',
    border: '1px solid #1a1a1a',
    backgroundColor: '#1a1a1a',
    color: '#fff',
    cursor: 'pointer',
  },
  inner: {
    maxWidth: '640px',
    margin: '0 auto',
    padding: '32px 24px 60px',
  },
  profileCard: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '20px 24px',
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    marginBottom: '20px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
  },
  avatar: {
    width: '48px',
    height: '48px',
    borderRadius: '50%',
    backgroundColor: '#e8c547',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '20px',
    fontWeight: '700',
    color: '#1a1a1a',
  },
  userName: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#1a1a1a',
  },
  userTag: {
    fontSize: '12px',
    color: '#888',
    marginTop: '2px',
  },
  section: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '24px',
    marginBottom: '16px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
  },
  sectionTitle: {
    fontSize: '15px',
    fontWeight: '700',
    color: '#1a1a1a',
    marginBottom: '16px',
  },
  emptyText: {
    textAlign: 'center',
    color: '#aaa',
    fontSize: '13px',
    padding: '20px 0',
  },
  formGroup: { marginBottom: '14px' },
  label: {
    display: 'block',
    fontSize: '13px',
    fontWeight: '500',
    marginBottom: '6px',
    color: '#444',
  },
  input: {
    width: '100%',
    padding: '10px 12px',
    border: '1px solid #ddd',
    borderRadius: '8px',
    fontSize: '14px',
    outline: 'none',
    boxSizing: 'border-box',
  },
  button: {
    width: '100%',
    padding: '11px',
    backgroundColor: '#1a1a1a',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  withdrawBtn: {
    width: '100%',
    padding: '11px',
    backgroundColor: '#fff',
    color: '#e24b4a',
    border: '1px solid #e24b4a',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  hint: {
    fontSize: '12px',
    color: '#888',
    marginTop: '8px',
  },
  error: {
    color: '#e24b4a',
    fontSize: '13px',
    marginBottom: '8px',
  },
  success: {
    color: '#3b7d22',
    fontSize: '13px',
    marginBottom: '8px',
  },
};

const resStyles: Record<string, React.CSSProperties> = {
  card: {
    border: '1px solid #e0e0e0',
    borderRadius: '8px',
    padding: '16px',
    marginBottom: '12px',
    backgroundColor: '#fafaf8',
  },
  top: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
  },
  badge: {
    padding: '2px 8px',
    borderRadius: '4px',
    fontSize: '11px',
    fontWeight: '600',
  },
  code: {
    fontSize: '12px',
    color: '#888',
    fontFamily: 'monospace',
  },
  concertTitle: {
    fontWeight: '600',
    fontSize: '15px',
    marginBottom: '4px',
    color: '#1a1a1a',
  },
  info: {
    fontSize: '13px',
    color: '#666',
    marginBottom: '12px',
  },
  bottom: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  price: {
    fontWeight: '700',
    fontSize: '15px',
    color: '#1a1a1a',
  },
  cancelBtn: {
    padding: '6px 14px',
    border: '1px solid #e24b4a',
    borderRadius: '6px',
    backgroundColor: '#fff',
    color: '#e24b4a',
    fontSize: '13px',
    cursor: 'pointer',
  },
};