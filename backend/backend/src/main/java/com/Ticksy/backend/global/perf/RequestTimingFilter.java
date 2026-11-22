package com.Ticksy.backend.global.perf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * [PERF] 좌석 배치도 조회의 서버 전체 처리 시간 측정 (perf 프로파일에서만 동작)
 *
 * serverMs  = 컨트롤러 + 서비스 + 트랜잭션 커밋(변경 감지) + JSON 직렬화 (JWT 인증 필터 이후 구간)
 * serviceMs = SeatService 로그의 값
 * → serverMs - serviceMs ≈ 트랜잭션 커밋 + JSON 직렬화 비용
 */
@Slf4j
@Component
@Profile("perf")
public class RequestTimingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !("GET".equals(request.getMethod()) && uri.endsWith("/seats"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long serverMs = (System.nanoTime() - start) / 1_000_000;
            log.info("[PERF] serverMs={} status={} uri={}?{}",
                    serverMs, response.getStatus(), request.getRequestURI(), request.getQueryString());
        }
    }
}
