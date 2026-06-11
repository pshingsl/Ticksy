package com.Ticksy.backend.global.auth.jwt;


import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j // 로그(log)를 출력할 수 있게 도와주는 롬복(lombok) 어노테이션
@Component // 스프링이 서버를 켤 때 이 클래스를 자동으로 발견해서 메모리에 객체로 등록하게 만듬
public class JwtProvider {

    // 1. yml 또는 .env 파일에 만든 환경변수 값을 읽어와서 자바 변수에 적용 후 사용
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // 실제 JWT 라이브러리가 암호화(서명) 연산을 할 떄 사용할 전용 객체(String 상태로는 쓸 수 없어서 변환용으로 선언)
    private SecretKey secretKey;

    // 2. 초기화 작업
    // @PostConstruct 스프링 위의 @Value 값들을 변수에 다 주입해 준 직 후,
    // 딱 한번 자동으로 실행되는 메서드이다.
    // 생성자 시점에는 @Value 값이 null이기 때문에 이 단계가 필요)
    @PostConstruct
    public void init() {
        // 일반 텍스트 문자(String)을 컴퓨터가 인식하는 표준바이트(Byte) 배열로 깨뜨린 뒤,
        // HMAC-SHA 알고리즘 규격에 맞는 진짜 '암호화용 비밀키 객체'로 변환하여 보관
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // Access token 생성
    // 사용자의 고유 ID(PK)와 역할(ADMIN, USER)을 내부에 안전하게 숨겨놓은 수명이 짧은 입장권을 생성
    public String createAccessToken(Long userId, String role) {
        return Jwts.builder() // JWT 생성을 위한 빌더를 연다.
                .subject(String.valueOf(userId)) // 토큰의 주인을 기록하는 자리(가장 중요한 데이터인 아이디를 문자열로 변환)
                .claim("role", role) // 개발자가 원하는 커스텀 데이터(유저, 관리자)를 키-값 형태로 마음꺼 집어넣는 공간
                .issuedAt(new Date()) // 토큰이 발행된 현재 시간을 기록
                .expiration(new Date(System.currentTimeMillis() + accessExpiration)) // 현재시간으로부터 30분동안 작동
                .signWith(secretKey) // 이끼 초기화 작업에서 만들어둔 암호화 키로 토큰 전체를 잠가버림 -> 위조 방지를 위해
                .compact(); // 위(53~58)까지의 정보들을 다 압축해서 최종적으로 "header.payload.signature" 형태의 긴 문자열로 반환
    }

    // Refresh token 생성
    // Access token이 만료되었을 떄 다시 새 로그인을 안 해도 되게끔 증증해 주느 수명이 긴 인증권 -> 최초로 입장이 성공하면 성공 기준을 7일동안 검증 필요 없이 입장가능
    // 내부에 권한(role) 같은 부가 정보는 넣지 않고, 오직 주인 확인을 위한 userId만 최소한으로 넣어서 보안을 높임.
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId)) // 토큰의 주인(userId)만 기록
                .issuedAt(new Date()) // 토큰이 발행된 시간을 기록
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration)) // 현재 시간으로부터 7일동안 작동
                .signWith(secretKey) // 아까 초기화 작업에서 초기화한 암호화 키로 토큰 전체를 잠근다.
                .compact(); // 위 정보들 압축하여 긴 문자열로 반환
    }

    // 토큰에서 userId 추출
    // 들어온 복잡한 토큰 문자열을 쪼개서 "이 토큰 주인의 회원 아이디 확인 가져옴"
    public Long getUserId(String token) {
        return Long.parseLong(
                getClaims(token).getSubject() // 밑에 있는 getClaims 기능으로 알맹이를 다 열고, 그 중 Subject(userId)를 꺼내 숫자를 변환
        );
    }

    // 토큰에서 role 추출
    // 토큰에서 사용자 권한 정보만 추출하는 기능
    public String getRole(String token) {
        // getClaims에서 role이라는 키값을 찾아서 꺼낸 뒤 문장(String) 형태로 변환한다.
        return getClaims(token).get("role", String.class);
    }

    // Access Token 검증
    public void validateAccessToken(String token) {
        try {
            getClaims(token); // 토큰 확인. 만약 위조되거나 만료되었다면 이 안에서 에러가 자동으로 발생
        } catch (ExpiredJwtException e) {
            // 만약 시간이 다 된 토큰이라면 라이브러리가 ExpiredJwtException을 던지고 아래 작성한 커스텀 에러 코드 실행
            throw new CustomException(ErrorCode.EXPIRED_ACCESS_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            // 시간이 다 된게 아니라, 누군가 위조 했거나 글자가 꺠진 유효하지 않은 토큰이면 예외 실행 후 커스텀 에러 코드 실행
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    // Refresh Token 검증
    public void validateRefreshToken(String token) {
        try {
            getClaims(token); // 토큰 확인. 만약 위조되거나 만료되었다면 이 안에서 에러가 자동으로 발생
        } catch (ExpiredJwtException e) {
            // 만약 시간이 다 된 토큰이라면 라이브러리가 ExpiredJwtException을 던지고 아래 작성한 커스텀 에러 코드 실행
            throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            // 시간이 다 된게 아니라, 누군가 위조 했거나 글자가 꺠진 유효하지 않은 토큰이면 예외 실행 후 커스텀 에러 코드 실행
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    // Claims 파싱
    // JWT 토큰의 내부 데이터를 파싱해 오는 로직
    // 이 클래스의 내부 데이터를 모든 검증과 추출 기능은 최종적으로 이 메서드를 거쳐서 작동
    private Claims getClaims(String token) {
        return Jwts.parser() // 토큰을 쪼개서 분석해 주는 파서(Parser)를 준비
                .verifyWith(secretKey) // 우리가 가지고 있는 비밀키 객체를 던져주어 "이 키로 잠근 게 맞는지 확인해 봐"라고 지정
                .build() // 파서 객체 빌드를 완료
                .parseSignedClaims(token) // 들어온 토큰 문자열을 비밀키로 해독하고 서명 위조 여부를 수학적으로 체크(위조 시 예외 터짐)
                .getPayload(); // 서명이 무사히 통과되었다면, 토큰 본문(Payload) 안에 담겨있는 진짜 데이터 조각(Claims)들을 꺼내 반환
    }
}
