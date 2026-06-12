import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  checkEmail,
  sendVerificationCode,
  verifyEmail,
  signup,
} from '../../api/auth';

type Step =
  | 'emailCheck'
  | 'codeSent'
  | 'verified'
  | 'done';

export default function SignupPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [name, setName] = useState('');

  const [step, setStep] = useState<Step>('emailCheck');
  const [emailChecked, setEmailChecked] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  // 이메일 중복 확인
  const handleCheckEmail = async () => {
    if (!email) {
      setError('이메일을 입력해주세요.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const data = await checkEmail(email);
      if (data.isDuplicate) {
        setError('이미 사용 중인 이메일입니다.');
        setEmailChecked(false);
      } else {
        setMessage('사용 가능한 이메일입니다.');
        setEmailChecked(true);
      }
    } catch {
      setError('이메일 확인 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 인증번호 발송
  const handleSendCode = async () => {
    if (!emailChecked) {
      setError('이메일 중복 확인을 먼저 해주세요.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await sendVerificationCode(email);
      setStep('codeSent');
      setMessage('인증번호를 발송했습니다. 이메일을 확인해주세요.');
    } catch (err: any) {
      const code = err.response?.data?.code;
      if (code === 'ALREADY_REGISTERED_EMAIL') {
        setError('이미 가입된 이메일입니다.');
      } else {
        setError('인증번호 발송 중 오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  // 인증번호 확인
  const handleVerifyCode = async () => {
    if (!code) {
      setError('인증번호를 입력해주세요.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await verifyEmail(email, code);
      setStep('verified');
      setMessage('이메일 인증이 완료되었습니다.');
    } catch (err: any) {
      const errCode = err.response?.data?.code;
      if (errCode === 'INVALID_VERIFICATION_CODE') {
        setError('인증번호가 일치하지 않습니다.');
      } else if (errCode === 'EXPIRED_VERIFICATION_CODE') {
        setError('인증번호가 만료되었습니다. 다시 요청해주세요.');
      } else {
        setError('인증번호 확인 중 오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  // 회원가입
  const handleSignup = async () => {
    if (step !== 'verified') {
      setError('이메일 인증을 완료해주세요.');
      return;
    }
    if (!password || !name) {
      setError('비밀번호와 이름을 입력해주세요.');
      return;
    }
    if (password !== passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다!');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await signup({ email, password, name });
      alert(`환영합니다, ${name}님!`);
      navigate('/login');
    } catch (err: any) {
      const errCode = err.response?.data?.code;
      if (errCode === 'DUPLICATE_EMAIL') {
        setError('이미 가입된 이메일입니다.');
      } else if (errCode === 'INVALID_VERIFICATION') {
        setError('이메일 인증이 만료되었습니다. 다시 인증해주세요.');
        setStep('emailCheck');
      } else {
        setError('회원가입 중 오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const isSignupActive =
    step === 'verified' &&
    password.length >= 8 &&
    password === passwordConfirm &&
    name.length > 0;

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <div style={styles.logo}>
          Tick<span style={styles.logoAccent}>sy</span>
        </div>
        <h2 style={styles.title}>회원가입</h2>

        {/* 이메일 */}
        <div style={styles.formGroup}>
          <label style={styles.label}>이메일</label>
          <div style={styles.row}>
            <input
              style={styles.input}
              type="email"
              placeholder="이메일 입력"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setEmailChecked(false);
                setStep('emailCheck');
                setMessage('');
              }}
              disabled={step === 'verified'}
            />
            <button
              style={styles.subButton}
              onClick={handleCheckEmail}
              disabled={loading || step === 'verified'}
            >
              중복확인
            </button>
          </div>
          {emailChecked && step === 'emailCheck' && (
            <div style={styles.row} >
              <button
                style={{
                  ...styles.subButton,
                  marginTop: '6px',
                  width: '100%',
                }}
                onClick={handleSendCode}
                disabled={loading}
              >
                인증번호 발송
              </button>
            </div>
          )}
        </div>

        {/* 인증번호 */}
        {(step === 'codeSent' || step === 'verified') && (
          <div style={styles.formGroup}>
            <label style={styles.label}>인증번호</label>
            <div style={styles.row}>
              <input
                style={styles.input}
                placeholder="인증번호 6자리 입력"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                disabled={step === 'verified'}
              />
              <button
                style={styles.subButton}
                onClick={handleVerifyCode}
                disabled={loading || step === 'verified'}
              >
                {step === 'verified' ? '인증완료 ✓' : '인증하기'}
              </button>
            </div>
          </div>
        )}

        {/* 비밀번호 */}
        <div style={styles.formGroup}>
          <label style={styles.label}>비밀번호</label>
          <input
            style={styles.input}
            type="password"
            placeholder="8자 이상, 특수문자 포함"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>

        {/* 비밀번호 확인 */}
        <div style={styles.formGroup}>
          <label style={styles.label}>비밀번호 확인</label>
          <input
            style={styles.input}
            type="password"
            placeholder="비밀번호 재입력"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
          />
          {passwordConfirm && password !== passwordConfirm && (
            <p style={styles.error}>
              비밀번호가 일치하지 않습니다!
            </p>
          )}
        </div>

        {/* 이름 */}
        <div style={styles.formGroup}>
          <label style={styles.label}>이름</label>
          <input
            style={styles.input}
            placeholder="실명을 입력하세요"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>

        {message && <p style={styles.success}>{message}</p>}
        {error && <p style={styles.error}>{error}</p>}

        <button
          style={{
            ...styles.button,
            backgroundColor: isSignupActive ? '#1a1a1a' : '#ccc',
            cursor: isSignupActive ? 'pointer' : 'not-allowed',
          }}
          onClick={handleSignup}
          disabled={!isSignupActive || loading}
        >
          {loading ? '처리 중...' : '회원가입'}
        </button>

        <div style={styles.links}>
          <Link to="/login" style={styles.link}>
            이미 계정이 있으신가요? 로그인
          </Link>
        </div>
      </div>
    </div>
  );
}

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
    maxWidth: '440px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.08)',
  },
  logo: {
    fontSize: '28px',
    fontWeight: '800',
    textAlign: 'center',
    marginBottom: '8px',
    color: '#1a1a1a',
  },
  logoAccent: { color: '#e8c547' },
  title: {
    fontSize: '18px',
    fontWeight: '600',
    textAlign: 'center',
    marginBottom: '24px',
    color: '#333',
  },
  formGroup: { marginBottom: '16px' },
  label: {
    display: 'block',
    fontSize: '13px',
    fontWeight: '500',
    marginBottom: '6px',
    color: '#444',
  },
  row: {
    display: 'flex',
    gap: '8px',
  },
  input: {
    flex: 1,
    padding: '10px 12px',
    border: '1px solid #ddd',
    borderRadius: '8px',
    fontSize: '14px',
    outline: 'none',
    boxSizing: 'border-box',
  },
  subButton: {
    padding: '10px 14px',
    backgroundColor: '#f5f5f0',
    border: '1px solid #ddd',
    borderRadius: '8px',
    fontSize: '13px',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  button: {
    width: '100%',
    padding: '12px',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    marginTop: '8px',
  },
  success: {
    color: '#3b7d22',
    fontSize: '13px',
    marginBottom: '8px',
  },
  error: {
    color: '#e24b4a',
    fontSize: '13px',
    marginTop: '4px',
  },
  links: {
    display: 'flex',
    justifyContent: 'center',
    marginTop: '16px',
  },
  link: {
    color: '#888',
    fontSize: '13px',
    textDecoration: 'none',
  },
};