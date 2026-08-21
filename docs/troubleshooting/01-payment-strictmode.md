### 1. React StrictMode 환경에서 결제 승인 중복 호출 문제

- **문제 상황:**
  Toss Payments 결제 성공 후 승인 결과 페이지(`PaymentCompletePage`) 진입 시 DB에는 예매 정보(`reservation`), 결제 내역(`payment`), 좌석(`reservation_seat`) 데이터가 정상 저장되었으나, 화면상에는 *"결제 승인에 실패했습니다. 고객센터에 문의해주세요"*라는 에러 메시지가 출력되는 현상 발생.

- **원인 분석:**
    1. 개발 환경의 `React.StrictMode`로 인해 컴포넌트 마운트 시 `useEffect` 내 결제 승인 API(`confirmPayment`)가 연속 **2회 호출**됨.
    2. **1차 요청:** 백엔드에서 Toss Payments 승인 완료 및 DB 저장 수행 후, Redis 내 임시 주문 정보(`orderRedisService.deleteOrderInfo(orderId)`) 삭제 완료.
    3. **2차 요청:** 이미 Redis에서 주문 정보가 삭제되었거나 Toss API 측에 승인 완료된 `orderId`로 재요청이 들어가 백엔드에서 예외(`PAYMENT_CONFIRM_FAILED`)가 발생하고, 이 응답이 최종 프론트 State에 반영되어 에러 화면 표출.

- **해결 방안:**
    - **프론트엔드 중복 요청 차단:** `useEffect` 내부에 `useRef` 플래그(`calledRef`)를 도입하여 컴포넌트 라이프사이클 내 승인 API 호출이 단 **1회만 수행**되도록 제어.

```typescript
// PaymentCompletePage.tsx
const calledRef = useRef(false);

useEffect(() => {
  // React.StrictMode에 의한 2번째 실행 차단
  if (calledRef.current) return;
  calledRef.current = true;

  const paymentKey = searchParams.get('paymentKey');
  const orderId = searchParams.get('orderId');
  const amount = searchParams.get('amount');

  if (paymentKey && orderId && amount) {
    handleConfirm(paymentKey, orderId, Number(amount));
  }
}, []);