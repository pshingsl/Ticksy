package com.Ticksy.backend.global.config;

import com.Ticksy.backend.global.auth.jwt.JwtFilter;
import com.Ticksy.backend.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity // 스프링 시큐리티의 모든 보안 필터 기능을 활성화시키는 어노테이션
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    /*
    * 스프링 시큐리티의 핵심(SecurityFilterChain)
    * 서버문 앞에 도달한 모든 HTTP 요청(Request)들이 순서대로 거쳐 가야 하는 '보안 검문소 성벽'을 세우는 Bean(객체) 메서드
    * http - 각 검문소의 세부 규칙을 람다식 형태로 조립할 수 있게 도와주는 스프링 시큐리티 핵심 조립 도구
    * */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 방식이라 불필요)
                // CSRF는 쿠키/세션 기반 보안에서 주로 쓰이는 공격 방어 기법
                // 서버 세션을 저장하는 방식이 아니라, 누구나 가질 수 있는 JWT 토큰을 헤더에 실어
                // 나르는 무상태(stateless) 방식이므로 이 방어가 불필요
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                // ㄴ 프론트엔드 서버(3000번 포트)와 백엔드 서버의 주소가 달라도 통신이 가능하게 문을 열어둠
                // ㄴ 기본적으로 브라우저는 주소가 다르면 보안상 데이터 요청을 차단
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용 안 함 (JWT Stateless)
                // 스프링 시큐리티의 기본 정책은 유저가 로그인하면 서버 메모리에 '세션'을 만들어 상주시킴
                // 로그인 했다는 증거를 서버 메모리에 두지 않고, 오직 클라이언트 들고 오는 JWT 토큰으로만 인증할 것이기 떄문
                // 모든 인증은 클라이언트가 가져오는 JWT 토큰 하나만 검사해서 그때그때 판단(Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 경로별 접근 제어
                // 어떤 URL 주소는 로그인 없이 통과시키고, 어떤 주소를 차단할지 결정
                .authorizeHttpRequests(auth -> auth

                        // 인증 불필요 경로
                        // 인증(로그인)이 전혀 필요 없는 경로 -> 비로그인자도 접근 가능
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/concerts/**",
                                "/swagger-ui/**",
                                "/api-docs/**"
                        ).permitAll() // 위의 주소들은 로그인 체크를 안 하고 전부 통과(permitAll()) 시킴

                        // 인증(로그인) 필요 경로 -> 인증 안되면 이용 불가능
                        .requestMatchers("/api/my/**").authenticated()
                        .requestMatchers("/api/seats/**").authenticated()
                        .requestMatchers("/api/reservations/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()

                        // 적어두지 않은 나머지 모든 API 요청들도 전부 로그인이 필요
                        .anyRequest().authenticated() // 위에 안 적은 자잘한 주소들도 100% 전부 무조건 인증(로그인)을 거쳐야만 통과
                )

                // JWT 필터 등록(JwtFilter를 시큐리티의 기본 필터 바로 앞에 장착)
                // 원래 시큐리티가 기본으로 쓰는 '아이디/비밀번호 검문소(UsernamePasswordAuthenticationFilter)'가 동작하기 직전에,
                // 'JWT 토큰 검문소(JwtFilter)'를 먼저 거치게 하여 토큰이 있으면 미리 통행증을 쥐어주어 통과시키는 메커니즘.
                .addFilterBefore(
                        new JwtFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build(); // 위 규칙대로 조립된 보안 성벽(FilterChain)을 반환
    }

    // 회원가입 시 사용자가 입력한 비밀번호(예: "1234")를 "양방향 해시 알고리즘(BCrypt)"을 이용해
    // 암호문(예: "$2a$10$X...")으로 변환해 주는 도구입니다. 디비에 비밀번호를 생짜로 저장하면 큰일 나기 때문에 필수입니다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * [CORS 세부 정책 설정 가이드]
     * 외부(프론트엔드)에서 백엔드 API를 찌를 때 어떤 규칙까지 허용해 줄지 상세하게 작성하는 공간.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 백엔드에 접속할 수 있는 프론트엔드 주소를 지정(
        config.setAllowedOrigins(List.of("http://localhost:3000"));

        // 프론트엔드가 보낼 수 있는 HTTP 메서드 종류들을 허용(Authorization 헤더에 JWT를 실어 보낼 것이므로 필수)
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        // 어떤 헤더 정보든 상관없이 전부 허용
        config.setAllowedHeaders(List.of("*"));

        //  쿠키나 인증 헤더 정보를 주고받는 것을 허용(true).
        config.setAllowCredentials(true);

        // 위의 크로스 도메인 규칙을 모든 URL 경로("/**")에 일괄 적용하겠다고 선언하고 반환
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}