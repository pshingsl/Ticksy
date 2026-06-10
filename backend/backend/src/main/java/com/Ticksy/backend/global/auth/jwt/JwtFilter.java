package com.Ticksy.backend.global.auth.jwt;

import com.Ticksy.backend.global.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor // final이 붙은 jwtProvider를 스프링이 자동으로 주입(의존성 주입)해 주도록 생성자를 생성
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    /**
     * 사용자가 API 요청을 보낼 때마다 자동으로 실행되는 핵심 필터 메서드
     * request: 들어온 요청 정보 (헤더, 주소 등)
     * response: 나갈 응답 정보
     * filterChain: 다음 문(필터)으로 통과시켜 주는 바통 체인 객체
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. 요청 헤더에서 사용자가 보낸 JWT 토큰 문자열 추출. (맨 밑의 resolveToken 메서드 실행)
        String token = resolveToken(request);

        // 2. 토큰이 비어 있지 않고 실제 텍스트가 잘 들어있다면 검증 및 인증 절차 시작
        if (StringUtils.hasText(token)) {
            try {
                //앞서 만든 JwtProvider를 이용해 이 토큰이 위조되었는지, 기간이 만료되었는지 정밀 검사
                jwtProvider.validateAccessToken(token);

                // 검증이 통과되었다면 토큰 Payload에 숨겨져 있던 회원 ID와 권한(Role)을 꺼낸다.
                Long userId = jwtProvider.getUserId(token);
                String role = jwtProvider.getRole(token);

                // [시큐리티 인증 객체 생성] 스프링 시큐리티가 이해할 수 있는 형태의 가짜 통행증(Authentication 객체)을 생성.
                // 첫 번째 인자: 로그인한 유저 정보(userId)
                // 두 번째 인자: 비밀번호 (JWT는 비밀번호 검증이 끝난 상태이므로 null 처리)
                // 세 번째 인자: 유저의 권한 목록 (스프링 시큐리티 규격인 "ROLE_USER", "ROLE_ADMIN" 형태로 가공해서 리스트로 주입)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                );

                // 스프링 시큐리티가 관리하는 전역 메모리 보관소(SecurityContext)에 이 가짜 통행증을 콕 박는다.
                // 이렇게 해두면 뒤이어 실행될 컨트롤러나 서비스 계층에서 "아, 이 요청은 인증된 회원이 보낸 거구나!"라고 인지하고 통과.
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (CustomException e) {
                // 만약 JwtProvider에서 유효하지 않은 토큰 에러가 터졌다면, 서버를 다운시키지 않고 로그만 조용히 남긴 뒤 통과.
                // (통과되더라도 보관소에 인증 객체가 안 들어갔기 때문에 뒷단 시큐리티 설정에서 자동으로 차단당함.)
                log.warn("JWT 인증 실패: {}", e.getMessage());
            }
        }

        // 3. 모든 검증 작업이 끝났으므로, 다음 필터(차단기) 혹은 실제 컨트롤러 API로 요청을 정상 패스(통과).
        filterChain.doFilter(request, response);
    }

    // HTTP 요청 헤더에서 JWT 토큰만 추출하는 기능
    // 보통 클라이언트는 요청 헤더에 "Authorization: Bearer eyJhbGciOi..." 형태로 토큰을 실어 보냄.
    private String resolveToken(HttpServletRequest request) {
        // HTTP 요청 헤더에서 "Authorization"이라는 이름의 값을 추출
        String bearerToken = request.getHeader("Authorization");

        // 값이 존재하고, 그 값이 반드시 "Bearer "라는 글자로 시작하는지 확인합니다. (JWT 표준 규격 규칙)
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // "Bearer " 가 총 7글자이므로, 앞의 7글자를 칼로 잘라내고 뒤에 남은 진짜 순수 JWT 토큰 문자열만 반환.
            return bearerToken.substring(7);
        }

        // 토큰이 없거나 규격이 안 맞으면 그냥 null을 반환합니다.
        return null;
    }
}
