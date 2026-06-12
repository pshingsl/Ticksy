import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../../api/auth';
import { useAuthStore } from '../../store/authStore';

export default function LoginPage() {
  // 페이지 이동을 명령하는 리액트 라우터 훅
  const navigate = useNavigate();
  // 전역 로그인 액션 함수를 꺼내와 이름을 storeLogin으로 별칭 지정
  const { login: storeLogin } = useAuthStore();

  // 지역상태 화면 내부에서만 실시간 입력값과 에러를 추적할 데이터보관함
  const [email, setEmail] = useState(''); // 입력할 이메일
  const [password, setPassword] = useState(''); // 입력할 비밀번호
  const [error, setError] = useState(''); // 화면에 출력할 에러
  const [loading, setLoading] = useState(false); // 현재 서버와 대화 중인지 여부 (로딩 바) 

  // 로그인 버튼 클릭 시 구동될 핵심 로직
  const handleLogin = async () => {
    // 만약 이메일이나 비번입력 안할 시 에러 발생
    if (!email || !password) {
      setError('이메일과 비밀번호를 입력해주세요.');
      return;
    }

    // 통신 시작되므로 로딩 중 상태로 스위치 On!
    setLoading(true);
    // 이전에 떠 있던 에러 메시지는 깔끔하게 지우기
    setError('');

    try {
      // auth.ts에 정의해 둔 로그인 함수를 호출 
      const data = await login(email, password);

      // 통신 성공 시 백엔드가 반환한 accessToken과 사용자 식별 정보(이메일 등)를 전역 창고에 추가
      storeLogin(data.accessToken, email);
      navigate('/');
    } catch (err: any) {
      // 백엔드가 에러 핸들러를 통해 던진 커스텀 에리 코드 해석
      const code = err.response?.data?.code;
      if (code === 'INVALID_CREDENTIALS') {
        setError('이메일 또는 비밀번호가 올바르지 않습니다.');
      } else if (code === 'DELETED_USER') {
        setError('탈퇴한 계정입니다.');
      } else {
        setError('로그인 중 오류가 발생했습니다.');
      }
    }

    // 성공하든 실패하든 대화가 끝났으니 로딩 상태를 Off 합니다.
    finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        {/* 서비스 로고 구역 */}
        <div style={styles.logo}>
          Tick<span style={styles.logoAccent}>sy</span>
        </div>
        <h2 style={styles.title}>로그인</h2>

        {/* 이메일 입력 그룹 */}
        <div style={styles.formGroup}>
          <label style={styles.label}>이메일</label>
          <input
            style={styles.input}
            type="email"
            placeholder="이메일을 입력하세요"
            value={email}
            // 유저가 키보드를 칠 때마다 바뀐 글자를 실시간으로 email 변수 주머니에 업데이트합니다.
            onChange={(e) => setEmail(e.target.value)}
            // 엔터키를 쳐도 로그인이 실행되도록 단축키 처리를 걸어둡니다.
            onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
          />
        </div>

        {/* 비밀번호 입력 그룹 */}
        <div style={styles.formGroup}>
          <label style={styles.label}>비밀번호</label>
          <input
            style={styles.input}
            type="password"
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
          />
        </div>
        {/* 🚨 에러가 발생했을 때만 화면에 빨간 글씨로 메시지를 렌더링하는 조건부 구문입니다. */}
        {error && <p style={styles.error}>{error}</p>}

        {/* 로그인 연동 버튼 */}
        <button
          style={{
            ...styles.button,
            opacity: loading ? 0.7 : 1,
            cursor: loading ? 'not-allowed' : 'pointer',
          }}
          onClick={handleLogin}
          disabled={loading}
        >
          {loading ? '로그인 중...' : '로그인'}
        </button>

        <div style={styles.links}>
          <Link to="/signup" style={styles.link}>
            회원가입
          </Link>
        </div>
      </div>
    </div>
  );
}

// 인라인 CSS 스타일 시트 객체
const styles: Record<string, React.CSSProperties> = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f5f5f0',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '40px',
    width: '100%',
    maxWidth: '420px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.08)',
  },
  logo: {
    fontSize: '28px',
    fontWeight: '800',
    textAlign: 'center',
    marginBottom: '8px',
    color: '#1a1a1a',
  },
  logoAccent: {
    color: '#e8c547',
  },
  title: {
    fontSize: '18px',
    fontWeight: '600',
    textAlign: 'center',
    marginBottom: '28px',
    color: '#333',
  },
  formGroup: {
    marginBottom: '16px',
  },
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
  error: {
    color: '#e24b4a',
    fontSize: '13px',
    marginBottom: '12px',
    textAlign: 'center',
  },
  button: {
    width: '100%',
    padding: '12px',
    backgroundColor: '#1a1a1a',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    marginTop: '8px',
  },
  links: {
    display: 'flex',
    justifyContent: 'center',
    marginTop: '16px',
    gap: '16px',
  },
  link: {
    color: '#888',
    fontSize: '13px',
    textDecoration: 'none',
  },
};