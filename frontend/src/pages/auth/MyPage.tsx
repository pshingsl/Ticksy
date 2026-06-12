import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { changePassword, withdraw, logout } from '../../api/auth';
import { useAuthStore } from '../../store/authStore';

export default function MyPage() {
  const navigate = useNavigate();
  // Zustand 창고에서 로그아웃 처리 함수와 현재 로그인 유저 이름을 실시간 연동
  const { logout: storeLogout, userName } = useAuthStore();

  // 비번 조작 폼 상태 관리(현재 비번, 새로운 비번, 새로운 비번 검사, 에러, 성공, 로딩)
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [pwError, setPwError] = useState('');
  const [pwSuccess, setPwSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // 로그아웃
  const handleLogout = async () => {
    try {
      await logout(); // 로그아웃 요청하면 백엔드 세션/레디스 토큰 파기 요청
    } finally {
      // 백엔드가 터지든 성공하든 상관없이 프론트엔드 장부(로컬스토리지)는 무조건 청소한다. (finally 구문 활용)
      storeLogout();
      navigate('/login');
    }
  };

  // 비밀번호 변경
  const handleChangePassword = async () => {
    // 1. 현재비번이랑 새로운 비번이 입력되지 않는다면 에러 호출
    if (!currentPassword || !newPassword) {
      setPwError('모든 항목을 입력해주세요.');
      return;
    }

    // 2. 새로운 비번 입력 후 새로운 비번 같은지 검사
    if (newPassword !== newPasswordConfirm) {
      setPwError('새 비밀번호가 일치하지 않습니다.');
      return;
    }
    const pwRegex =
      /^(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/;
    if (!pwRegex.test(newPassword)) {
      setPwError(
        '비밀번호는 8자 이상이며 특수문자를 포함해야 합니다.'
      );
      return;
    }

    setLoading(true);
    setPwError('');
    setPwSuccess('');

    try {
      // 백엔드로 구 패스워드와 신 패스워드를 묶어 전송
      await changePassword(currentPassword, newPassword);
      setPwSuccess(
        '비밀번호가 변경되었습니다. 다시 로그인해주세요.'
      );
      // 디바운스/타이머 효과: 유저가 성공 메시지를 읽을 시간을 2초간 준 뒤 가입 창고를 부수고 로그아웃 처리
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
      setLoading(false);
    }
  };

  // 회원 탈퇴
  const handleWithdraw = async () => {
    // 브라우저의 기본 확인창(Confirm)을 띄워 유저의 고의성 의사를 한 번 더 심사
    const confirmed = window.confirm(
      '정말 탈퇴하시겠습니까?'
    );
    if (!confirmed) return; // '취소'를 누르면 즉시 함수를 자진 중단합니다.

    setLoading(true);
    try { 
      await withdraw(); // 백엔드 DB 유저 Soft/Hard Delete 트리거 호출
      alert('탈퇴가 완료되었습니다.');
      storeLogout();
      navigate('/login');
    } catch (err: any) {
      const code = err.response?.data?.code;
      // 무결성 예외 처리: 만약 백엔드에서 예매 정보 연관 관계가 묶여있어 에러를 뱉었다면 분기 안내
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
      <div style={styles.inner}>
        {/* 헤더 */}
        <div style={styles.header}>
          <div style={styles.logo}>
            Tick<span style={styles.logoAccent}>sy</span>
          </div>
          <button style={styles.logoutBtn} onClick={handleLogout}>
            로그아웃
          </button>
        </div>

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
              onChange={(e) =>
                setNewPasswordConfirm(e.target.value)
              }
            />
          </div>

          {pwError && <p style={styles.error}>{pwError}</p>}
          {pwSuccess && <p style={styles.success}>{pwSuccess}</p>}

          <button
            style={{
              ...styles.button,
              opacity: loading ? 0.7 : 1,
            }}
            onClick={handleChangePassword}
            disabled={loading}
          >
            비밀번호 변경
          </button>
          <p style={styles.hint}>
            ※ 변경 후 보안을 위해 자동 로그아웃됩니다.
          </p>
        </div>

        {/* 회원 탈퇴 */}
        <div style={styles.section}>
          <h3 style={styles.sectionTitle}>회원 탈퇴</h3>
          <p style={styles.hint}>
            탈퇴 시 계정 복구가 불가능합니다.
            확정된 예매 내역이 있으면 탈퇴할 수 없습니다.
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
    padding: '0 16px',
  },
  inner: {
    maxWidth: '600px',
    margin: '0 auto',
    paddingBottom: '40px',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '20px 0',
    borderBottom: '1px solid #e0e0e0',
    marginBottom: '24px',
  },
  logo: {
    fontSize: '22px',
    fontWeight: '800',
    color: '#1a1a1a',
  },
  logoAccent: { color: '#e8c547' },
  logoutBtn: {
    padding: '8px 16px',
    backgroundColor: '#1a1a1a',
    color: '#fff',
    border: 'none',
    borderRadius: '6px',
    fontSize: '13px',
    cursor: 'pointer',
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