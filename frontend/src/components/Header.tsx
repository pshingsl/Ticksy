import { useNavigate, Link } from "react-router-dom";
import { useState } from "react";
import { useAuthStore } from "../store/authStore";
import { logout } from "../api/auth";

export default function Header() {
  const navigate = useNavigate();
  const { isLoggedIn, userName, logout: storeLogout } = useAuthStore();
  const [keyword, setKeyword] = useState('');

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      storeLogout();
      navigate('/login');
    }
  };

  const handleSearch = () => {
    if (!keyword.trim()) return;
    navigate(`/concerts?keyword=${encodeURIComponent(keyword)}`);
  };

  return (
    <header style={styles.header}>
      <Link to="/" style={styles.logo}>
        Tick<span style={styles.logoAccent}>sy</span>
      </Link>

      <div style={styles.searchBox}>
        <span style={styles.searchIcon}>🔍</span>
        <input style={styles.searchInput}
          placeholder="공연명, 아티스트 검색" value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
        />
      </div>


      <div style={styles.actions}>
        {isLoggedIn ? (
          <>
            <span style={styles.userName}>{userName}님</span>
            <Link to="/mypage" style={styles.btn}>
              마이페이지
            </Link>
            <button style={styles.btnPrimary} onClick={handleLogout}>
              로그아웃
            </button>
          </>
        ) : (
          <>
            <Link to="/login" style={styles.btn}>
              로그인
            </Link>
            <Link to="/signup" style={styles.btnPrimary}>
              회원가입
            </Link>
          </>
        )}
      </div>
    </header>
  )
}

const styles: Record<string, React.CSSProperties> = {
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
    zIndex: 100
  },
  logo: {
    fontSize: '20px',
    fontWeight: '800',
    color: '#1a1a1a',
    textDecoration: 'none',
  },
  logoAccent: { color: '#e8c547' },
  searchBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    border: '1px solid #ddd',
    borderRadius: '20px',
    padding: '6px 14px',
    width: '320px',
},
searchIcon: { color: '#aaa', fontSize: '14px' },
  searchInput: {
    border: 'none',
    outline: 'none',
    fontSize: '13px',
    width: '100%',
    background: 'transparent',
  },
  actions: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
  },
  userName: {
    fontSize: '13px',
    color: '#555',
    marginRight: '4px',
  },
  btn: {
    padding: '7px 16px',
    borderRadius: '6px',
    fontSize: '13px',
    border: '1px solid #ddd',
    backgroundColor: '#fff',
    color: '#333',
    textDecoration: 'none',
    cursor: 'pointer',
  },
  btnPrimary: {
    padding: '7px 16px',
    borderRadius: '6px',
    fontSize: '13px',
    border: '1px solid #1a1a1a',
    backgroundColor: '#1a1a1a',
    color: '#fff',
    textDecoration: 'none',
    cursor: 'pointer',
  },
};