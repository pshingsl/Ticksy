# Ticksy 

콘서트 티켓 플랫폼

## 🎯 프로젝트 소개

### 서비스 설명
Ticksy는 트래픽이 집중되는 콘서트/공연 환경에서 **실시간 좌석 선점**과 **안정적인 결제 프로세스**를 제공하는 티켓 예매 플랫폼입니다.
사용자는 실시간 좌석 배치도를 확인하여 원하는 좌석을 5분간 임시 선점할 수 있으며, Toss Payments 연동을 통해 결제 및 예매 확정까지 단절 없는 흐름으로 진행됩니다.

## 🔥 핵심 기술적 도전
### 1. Redis 분산 락을 이용한 좌석 선점 동시성 제어
### 2. Toss Payments 결제 연동 플로우

## 🛠 Tech Stack
### Backend
<img src="https://img.shields.io/badge/Java 21-007396?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/Spring Boot 4.0.6-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"><img src="https://img.shields.io/badge/Spring Security 7.0.x-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"> <img src="https://img.shields.io/badge/Spring Data JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white"> <img src="https://img.shields.io/badge/Spring Data Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white"> <img src="https://img.shields.io/badge/Spring Boot Actuator-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"><img src="https://img.shields.io/badge/JWT 0.12.5-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"> <img src="https://img.shields.io/badge/Redisson 3.43.0-DC382D?style=for-the-badge&logo=redis&logoColor=white"> <img src="https://img.shields.io/badge/Swagger (SpringDoc 3.0.3)-85EA2D?style=for-the-badge&logo=swagger&logoColor=black">

### Frontend
<img src="https://img.shields.io/badge/React 19.x-20232A?style=for-the-badge&logo=react&logoColor=61DAFB"> <img src="https://img.shields.io/badge/TypeScript 5.x-007ACC?style=for-the-badge&logo=typescript&logoColor=white"> <img src="https://img.shields.io/badge/React Router 7.x-CA4245?style=for-the-badge&logo=react-router&logoColor=white"> <img src="https://img.shields.io/badge/Axios 1.x-5A29E4?style=for-the-badge&logo=axios&logoColor=white"> <img src="https://img.shields.io/badge/Zustand 5.x-000000?style=for-the-badge&logo=Zustand&logoColor=white">

### Database
<img src="https://img.shields.io/badge/MySQL 8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/Redis 7.x-DC382D?style=for-the-badge&logo=redis&logoColor=white">

### Infra
<img src="https://img.shields.io/badge/Apache JMeter 5.6.x-D22128?style=for-the-badge&logo=apachejmeter&logoColor=white">

### Test
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/Docker Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white">

## 🏗 아키텍처 및 ERD

<details>

<summary>아키텍처</summary>

- 흐름 설명 (이미지 없으면 텍스트)
</details>

<br>

<details>

<summary>ERD</summary>

[링크](https://www.erdcloud.com/d/Yzguo7ihDa2M5aWQR)

![img_1.png](img_1.png)
</details>

## ✨ 주요 기능
- 공연 목록/검색/상세 조회
- 좌석 배치도 조회 (DB + Redis 조합)
- 좌석 선점 (Redisson 분산 락 + TTL 5분)
- Toss Payments 결제 / 예매 취소 + 환불
- JWT 인증 (Access/Refresh Token)

## ⚡ Performance Optimization
### Before (문제 상황)
### 구조 개선 (방식1 - 구역별 분리 조회)
### After (개선 결과)
### 인덱스 적용 전/후 수치 비교 표

## 🔥 트러블슈팅
### 1. Redisson 버전 호환 문제
### 2. LAZY 로딩 HOLDING 미표시
### 3. Redis 키 공백 버그
### 4. orderId 중복 생성
### 5. HikariCP 커넥션 풀 고갈
### 6. React StrictMode 결제 중복 호출
### 7. Toss orderId 불일치

## 📄 API Docs
- Swagger 링크 또는 대표 API 목록

## 🚀 Getting Started
- 실행 방법
- 환경 변수

## 📁 Project Structure

```java
com.Ticksy.backend
├── domain
│   ├── user
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   ├── concert
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   ├── seat
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   ├── reservation
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   └── payment
│       ├── controller
│       ├── service
│       ├── repository
│       ├── entity
│       └── dto
└── global
    ├── config
    │   ├── SecurityConfig.java
    │   ├── RedisConfig.java
    │   ├── SwaggerConfig.java
    │   └── CloudinaryConfig.java
    ├── auth
    │   ├── jwt
    │   │   ├── JwtProvider.java
    │   │   └── JwtFilter.java
    │   └── oauth2
    │       ├── CustomOAuth2UserService.java
    │       └── OAuth2SuccessHandler.java
    ├── exception
    │   ├── GlobalExceptionHandler.java
    │   ├── CustomException.java
    │   └── ErrorCode.java
    └── response
        └── ApiResponse.java
```