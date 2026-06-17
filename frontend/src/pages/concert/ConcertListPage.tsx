import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Header from '../../components/Header';
import { getConcertList, searchConcerts } from '../../api/concert';
import { ConcertListItem } from '../../types/concert';

export default function ConcertListPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const keyword = searchParams.get('keyword');

  const [concerts, setConcerts] = useState<ConcertListItem[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  // 필터
  const [date, setDate] = useState('');
  const [region, setRegion] = useState('');

  useEffect(() => {
    fetchConcerts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, keyword, date, region]);

  const fetchConcerts = async () => {
    setLoading(true);
    try {
      let data;
      if (keyword) {
        data = await searchConcerts(keyword, page, 12);
      } else {
        data = await getConcertList({
          date: date || undefined,
          region: region || undefined,
          page,
          size: 12,
        });
      }
      setConcerts(data.content);
      setTotalElements(data.totalElements);
      setTotalPages(data.totalPages);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const formatPrice = (min: number, max: number) => {
    if (min === max) return `${min.toLocaleString()}원`;
    return `${min.toLocaleString()} ~ ${max.toLocaleString()}원`;
  };

  return (
    <div style={styles.page}>
      <Header />
      <div style={styles.container}>
        {keyword && (
          <div style={styles.searchInfo}>
            '{keyword}' 검색 결과{' '}
            <strong>{totalElements}건</strong>
          </div>
        )}

        {!keyword && (
          <div style={styles.filterBar}>
            <input
              type="date"
              style={styles.filterSelect}
              value={date}
              onChange={(e) => {
                setDate(e.target.value);
                setPage(0);
              }}
            />
            <select
              style={styles.filterSelect}
              value={region}
              onChange={(e) => {
                setRegion(e.target.value);
                setPage(0);
              }}
            >
              <option value="">지역 선택</option>
              <option value="서울">서울</option>
              <option value="경기">경기</option>
              <option value="인천">인천</option>
            </select>
            {(date || region) && (
              <button
                style={styles.resetBtn}
                onClick={() => {
                  setDate('');
                  setRegion('');
                  setPage(0);
                }}
              >
                필터 초기화
              </button>
            )}
          </div>
        )}

        {loading ? (
          <div style={styles.loading}>로딩 중...</div>
        ) : concerts.length === 0 ? (
          <div style={styles.empty}>
            {keyword
              ? '검색 결과가 없습니다.'
              : '등록된 공연이 없습니다.'}
          </div>
        ) : (
          <div style={styles.grid}>
            {concerts.map((concert) => (
              <div
                key={concert.concertId}
                style={styles.card}
                onClick={() =>
                  navigate(`/concerts/${concert.concertId}`)
                }
              >
                <div style={styles.poster}>
                  {concert.posterUrl ? (
                    <img
                      src={concert.posterUrl}
                      alt={concert.title}
                      style={styles.posterImg}
                    />
                  ) : (
                    <span style={styles.posterPlaceholder}>
                      포스터 이미지
                    </span>
                  )}
                  {!concert.hasAvailableSeat && (
                    <span style={styles.badgeSoldOut}>매진</span>
                  )}
                </div>
                <div style={styles.info}>
                  <div style={styles.title}>{concert.title}</div>
                  <div style={styles.venue}>{concert.venueName}</div>
                  {concert.eventDate && (
                    <div style={styles.date}>
                      {concert.eventDate}
                    </div>
                  )}
                  <div style={styles.price}>
                    {formatPrice(concert.minPrice, concert.maxPrice)}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {totalPages > 1 && (
          <div style={styles.pagination}>
            <button
              style={styles.pageBtn}
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              ◀
            </button>
            {Array.from({ length: totalPages }, (_, i) => (
              <button
                key={i}
                style={{
                  ...styles.pageBtn,
                  ...(i === page ? styles.pageBtnActive : {}),
                }}
                onClick={() => setPage(i)}
              >
                {i + 1}
              </button>
            ))}
            <button
              style={styles.pageBtn}
              disabled={page === totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              ▶
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: { minHeight: '100vh', backgroundColor: '#f5f5f0' },
  container: { maxWidth: '1200px', margin: '0 auto', padding: '32px 40px' },
  searchInfo: {
    fontSize: '13px',
    color: '#888',
    marginBottom: '16px',
  },
  filterBar: {
    display: 'flex',
    gap: '10px',
    marginBottom: '24px',
    flexWrap: 'wrap',
  },
  filterSelect: {
    padding: '7px 12px',
    border: '1px solid #ddd',
    borderRadius: '6px',
    fontSize: '13px',
    backgroundColor: '#fff',
  },
  resetBtn: {
    padding: '7px 12px',
    border: '1px solid #ddd',
    borderRadius: '6px',
    fontSize: '13px',
    backgroundColor: '#fff',
    cursor: 'pointer',
    color: '#888',
  },
  loading: {
    textAlign: 'center',
    padding: '80px 0',
    color: '#888',
    fontSize: '14px',
  },
  empty: {
    textAlign: 'center',
    padding: '80px 0',
    color: '#888',
    fontSize: '14px',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
    gap: '16px',
  },
  card: {
    backgroundColor: '#fff',
    border: '1px solid #e0e0e0',
    borderRadius: '10px',
    overflow: 'hidden',
    cursor: 'pointer',
    transition: 'box-shadow 0.2s',
  },
  poster: {
    backgroundColor: '#e8e8e0',
    height: '220px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
    overflow: 'hidden',
  },
  posterImg: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  posterPlaceholder: {
    color: '#999',
    fontSize: '12px',
  },
  badgeSoldOut: {
    position: 'absolute',
    top: '8px',
    right: '8px',
    backgroundColor: '#fcebeb',
    color: '#a32d2d',
    fontSize: '11px',
    fontWeight: '600',
    padding: '2px 8px',
    borderRadius: '4px',
  },
  info: { padding: '12px' },
  title: {
    fontWeight: '600',
    fontSize: '13px',
    marginBottom: '4px',
    color: '#1a1a1a',
  },
  venue: { color: '#666', fontSize: '12px', marginBottom: '2px' },
  date: { color: '#888', fontSize: '11px', marginBottom: '4px' },
  price: { fontSize: '12px', fontWeight: '600', color: '#1a1a1a' },
  pagination: {
    display: 'flex',
    gap: '4px',
    justifyContent: 'center',
    marginTop: '32px',
  },
  pageBtn: {
    width: '32px',
    height: '32px',
    border: '1px solid #ddd',
    borderRadius: '6px',
    backgroundColor: '#fff',
    fontSize: '13px',
    cursor: 'pointer',
  },
  pageBtnActive: {
    backgroundColor: '#1a1a1a',
    color: '#fff',
    borderColor: '#1a1a1a',
  },
};