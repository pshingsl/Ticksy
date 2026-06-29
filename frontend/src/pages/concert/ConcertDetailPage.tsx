import { useEffect, useState } from 'react'; // 리액트 Hook(기능)
import { useNavigate, useParams } from 'react-router-dom'; // 페이지 이동 및 주소창 변수 읽기용
import Header from '../../components/Header'; // 공통 상단  메뉴 바 컴포넌트
import { getConcertDetail } from '../../api/concert'; // 서버에 공연 상세 데이터를 요청하는 비동기 함수
import { ConcertDetail } from '../../types/concert'; // TypeScript용 데이터 타입 정의서
import { useAuthStore } from '../../store/authStore'; // 로그인 상태를 관리하는 전역 저장소

export default function ConcertDetailPage() {
  // 도구 및 상태(state) 선언 영역
  
  // 주소창의 파라미터를 읽기위한 변수 선언
  // useParams(주소창 변수 가로채기)
  // 라우터 주소가 `/도메인/숫자` 혈태로 설계 되어 있을 때, 
  // 실제 주소창에 들어온 `/concert/45`에서 `45`라는 변수 데이터를 쏙 빼오는 도구
  const { concertId } = useParams();
  
  // 다른 페이지로 강제 이동시켜주는 네비게이터 함수 생성
  // useNavigator: HTML의 기본 링크이동(<a href = "...">)는 환면 전체를 새로고침하여 앱을 느리게 만든다.
  // 이를 막기 위해 자바스크립트 코드 내부에서 부드럽게 페이지를 주소창만 바꿔서 이동시켜주는 도구이다.
  const navigate = useNavigate();

  // 로그인 여부를 확인 변수 선언 -> 전역 로그인 저장소 사용
  const { isLoggedIn } = useAuthStore();

  // 서버에 가져온 콘서트 상세 정보를 저장할 상자(처음엔 데이터가 없으니 null))
  // useState: 화면이 켜져 있는 동안 계속 유지되고, 값이 바뀌면 화면을 자동으로 다시 그리게 만드는 변수
  const [concert, setConcert] = useState<ConcertDetail | null>(null);

  // 로딩 중인지 화면에 표시할 스위치 상자(기본값은 로딩 중이므로 참으로 설정)
  const [loading, setLoading] = useState(true);

  // 에러가 발생하는지 화면에 표시할 메시지 상자(처음엔 빈 문자열)
  const [error, setError] = useState('');

  /**
   * useState(화면 제어 전용 타이머/스위치)
   * 개념: 컴포넌트가 처음 화면에 나타날 때, 사라질 떄, 혹은 특정 변수가 변할 때마다 
   * 자동으로 실행하고 싶은 **주변 작업(Side Effect)**을 처리
   * 
   * useEffect(() => {
   * console.log("화면이 처음 켜질 때 딱 한 번 실행됩니다!");
   * }, []); // 뒤에 빈 배열 []을 붙이면 '처음 켜질 때만' 실행하라는 뜻
   */
  useEffect(() => {
    fetchDetail();
    
  }, [concertId]);

  const fetchDetail = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getConcertDetail(Number(concertId));
      setConcert(data);
    } catch (err: any) {
      if (err.response?.data?.code === 'NOT_FOUND_CONCERT') {
        setError('존재하지 않는 공연입니다.');
      } else {
        setError('공연 정보를 불러오지 못했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleScheduleClick = (schedule: ConcertDetail['schedules'][0]) => {
    if (!isLoggedIn) {
      alert('로그인을 해주세요.');
      navigate('/login');
      return;
    }

    if (schedule.status === 'UPCOMING') {
      alert(
        `예매 오픈 전입니다.\n오픈 일시: ${formatDateTime(
          schedule.bookingOpenAt
        )}`
      );
      return;
    }

    if (schedule.status === 'SOLD_OUT' || schedule.remainingSeatCount === 0) {
      alert('매진된 회차입니다.');
      return;
    }

    // concertId 저장 후 이동
    localStorage.setItem('currentConcertId', String(concert?.concertId));

    // 좌석 배치도 페이지로 이동 (좌석 도메인 구현 후 연결)
    navigate(`/seats/${schedule.scheduleId}`);
  };

  const formatDateTime = (iso: string) => {
    const d = new Date(iso);
    return `${d.getMonth() + 1}월 ${d.getDate()}일 ${d.getHours()}시`;
  };

  const formatDate = (dateStr: string) => {
    const d = new Date(dateStr);
    const days = ['일', '월', '화', '수', '목', '금', '토'];
    return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일 (${
      days[d.getDay()]
    })`;
  };

  if (loading) {
    return (
      <div style={styles.page}>
        <Header />
        <div style={styles.loading}>로딩 중...</div>
      </div>
    );
  }

  if (error || !concert) {
    return (
      <div style={styles.page}>
        <Header />
        <div style={styles.loading}>{error}</div>
      </div>
    );
  }

  return (
    // div: 디자인적 구역이나 행/열 레이아웃을 묶을 때 사용하는 가장 대중적인 태크. 줄바꿈이 기본적으로 적용
    <div style={styles.page}>  
      <Header />
      <div style={styles.container}>
        {/* 공연 정보 영역 */}
        <div style={styles.infoSection}>
          <div style={styles.posterLg}>
            {concert.posterUrl ? (
              <img
                src={concert.posterUrl}
                alt={concert.title}
                style={styles.posterImg}
              />
            ) : (
              <span style={styles.posterPlaceholder}>포스터 이미지</span>
            )}
          </div>
          <div style={styles.infoText}>
            <h1 style={styles.title}>{concert.title}</h1>
            <div style={styles.metaRow}>출연진: {concert.cast || '-'}</div>
            <div style={styles.metaRow}>공연장: {concert.venueName}</div>
            <div style={styles.metaRow}>
              관람연령: {concert.ageLimit || '-'}
            </div>
            <div style={styles.metaRow}>
              관람시간: {concert.runTime ? `${concert.runTime}분` : '-'}
            </div>

            <hr style={styles.divider} />

            <div style={styles.gradeTitle}>등급별 가격</div>
            <div style={styles.gradeRow}>
              {concert.grades.map((g) => (
                <div key={g.grade} style={styles.gradeCard}>
                  <div style={styles.gradeName}>{g.grade}</div>
                  <div style={styles.gradePrice}>
                    {g.price.toLocaleString()}원
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <hr style={styles.divider} />

        {/* 공연 소개 */}
        <div style={styles.section}>
          <div style={styles.sectionTitle}>공연 소개</div>
          <div style={styles.description}>
            {concert.description || '공연 소개가 없습니다.'}
          </div>
        </div>

        {/* 회차 선택 */}
        <div style={styles.section}>
          <div style={styles.sectionTitle}>회차 선택</div>
          <div style={styles.scheduleList}>
            {concert.schedules.map((schedule) => (
              <div key={schedule.scheduleId} style={styles.scheduleCard}>
                <div>
                  <div style={styles.scheduleDate}>
                    {formatDate(schedule.eventDate)}{' '}
                    {schedule.eventTime.slice(0, 5)}
                  </div>

                  {schedule.status === 'UPCOMING' ? (
                    <div style={styles.scheduleMeta}>
                      예매 오픈:{' '}
                      {formatDateTime(schedule.bookingOpenAt)}{' '}
                      <span style={styles.badgeSoon}>오픈예정</span>
                    </div>
                  ) : (
                    <div style={styles.scheduleMeta}>
                      잔여 좌석:{' '}
                      {schedule.remainingSeatCount === 0 ? (
                        <span style={{ color: '#a32d2d' }}>
                          0석 (매진)
                        </span>
                      ) : (
                        <strong>{schedule.remainingSeatCount}석</strong>
                      )}
                    </div>
                  )}
                </div>

                <button
                  style={
                    schedule.status === 'UPCOMING' ||
                    schedule.remainingSeatCount === 0
                      ? styles.btnDisabled
                      : styles.btnPrimary
                  }
                  onClick={() => handleScheduleClick(schedule)}
                  disabled={
                    schedule.status === 'UPCOMING' ||
                    schedule.remainingSeatCount === 0
                  }
                >
                  {schedule.status === 'UPCOMING'
                    ? '오픈 전'
                    : schedule.remainingSeatCount === 0
                    ? '매진'
                    : '예매하기'}
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: { minHeight: '100vh', backgroundColor: '#f5f5f0' },
  container: { maxWidth: '1200px', margin: '0 auto', padding: '32px 40px' },
  loading: {
    textAlign: 'center',
    padding: '80px 0',
    color: '#888',
    fontSize: '14px',
  },
  infoSection: {
    display: 'flex',
    gap: '32px',
    marginBottom: '40px',
  },
  posterLg: {
    width: '240px',
    height: '320px',
    backgroundColor: '#e8e8e0',
    borderRadius: '8px',
    flexShrink: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  posterImg: { width: '100%', height: '100%', objectFit: 'cover' },
  posterPlaceholder: { color: '#999', fontSize: '12px' },
  infoText: { flex: 1 },
  title: {
    fontSize: '22px',
    fontWeight: '700',
    marginBottom: '12px',
    color: '#1a1a1a',
  },
  metaRow: { fontSize: '13px', color: '#666', marginBottom: '6px' },
  divider: { border: 'none', borderTop: '1px solid #e0e0e0', margin: '24px 0' },
  gradeTitle: {
    fontWeight: '600',
    fontSize: '13px',
    marginBottom: '8px',
    marginTop: '8px',
  },
  gradeRow: { display: 'flex', gap: '10px', flexWrap: 'wrap' },
  gradeCard: {
    backgroundColor: '#fff',
    border: '1px solid #e0e0e0',
    borderRadius: '8px',
    padding: '10px 16px',
    minWidth: '100px',
  },
  gradeName: { fontSize: '11px', color: '#888', marginBottom: '4px' },
  gradePrice: { fontSize: '14px', fontWeight: '600', color: '#1a1a1a' },
  section: { marginBottom: '32px' },
  sectionTitle: {
    fontSize: '18px',
    fontWeight: '700',
    marginBottom: '16px',
    color: '#1a1a1a',
  },
  description: {
    backgroundColor: '#fff',
    border: '1px solid #e0e0e0',
    borderRadius: '8px',
    padding: '16px',
    fontSize: '13px',
    color: '#555',
    lineHeight: '1.7',
  },
  scheduleList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
  },
  scheduleCard: {
    backgroundColor: '#fff',
    border: '1px solid #e0e0e0',
    borderRadius: '8px',
    padding: '14px 16px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  scheduleDate: {
    fontWeight: '600',
    fontSize: '13px',
    color: '#1a1a1a',
    marginBottom: '4px',
  },
  scheduleMeta: { fontSize: '12px', color: '#888' },
  badgeSoon: {
    backgroundColor: '#faeeda',
    color: '#854f0b',
    fontSize: '11px',
    padding: '2px 6px',
    borderRadius: '4px',
    marginLeft: '4px',
  },
  btnPrimary: {
    padding: '8px 20px',
    backgroundColor: '#e8c547',
    color: '#1a1a1a',
    border: 'none',
    borderRadius: '6px',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  btnDisabled: {
    padding: '8px 20px',
    backgroundColor: '#f5f5f0',
    color: '#aaa',
    border: 'none',
    borderRadius: '6px',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'not-allowed',
  },
};