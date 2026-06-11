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
@EnableWebSecurity // 스프링 시큐리티의 웹 보안 기능(인증/인가 차단기)을 통째로 켜고 활성화
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
                /*
                 * CSRF(사이트 간 요청 위조) 보호 기능 비활성화 (JWT 방식이라 불필요) -> 보안 기능 끄기
                 * CSRF는 사용자의 브라우저가 자동으로 인증 정보를 보내는 특징을 악용한 공격
                 *ㄴ쿠키/세션 기반 보안에서 주로 쓰이는 공격 방어 기법
                 * 현재 프로젝트
                 * ㄴ서버에 아무것도 저장하지 않곡 헤더에 JWT> 토큰을 실어 보내는 무상태(stateless) 방식
                 * ㄴ이 방어 필터가 켜져 있으면 오히려 토큰 요청을 변조로 오해해서 막아버린다 -> 따라서 과감히 disable  처리
                 * */
                .csrf(AbstractHttpConfigurer::disable)

                /* CORS(교차 출처 자원 공유) 정책 연동하기
                 * ㄴ 프론트엔드 서버(3000번 포트)와 백엔드 서버(8080)의 주소가 달라도 통신이 가능하게 문을 열어둠
                 * ㄴ 기본적으로 브라우저는 주소가 다르면 보안상 데이터 요청을 차단
                 * ㄴ 맨 아래의 우리가 따로 정의해 놓은 둔 규칙(configurationSource 103번째줄)을 보안을 공식적으로 장착시키는 과정
                 */
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                /* 세션 정책을 무상태(Stateless)로 강제 고정
                 * 스프링 시큐리티의 기본 정책은 유저가 로그인하면 서버 메모리에 '세션'을 만들어 저장시킴
                 *
                 * 현재 프로젝트:
                 * 로그인 했다는 증거를 서버 메모리에 두지 않고, 오직 클라이언트 들고 오는 JWT 토큰으로만 인증할 것이기 떄문
                 *
                 * 따라서
                 * - 서버 메모리를 가볍게 유지하고 대규모 접속을 버티기 위해 세션을 절잳 만들지도 쓰지 않겠다고 명시
                 * ㄴ 서버 메모리에 로그인 상태 저장
                 * ㄴ 요청마다 JWT 새로 검증
                 * ㄴ 완전 무상태(Stateless) 방식 사용
                 * ㄴ모든 인증은 클라이언트가 가져오는 JWT 토큰 하나만 검사해서 그때그때 판단(Stateless)
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 경로별 접근 제어
                // 어떤 URL 주소는 로그인 없이 통과시키고, 어떤 주소를 통과 않 시킬지 결정
                .authorizeHttpRequests(auth -> auth

                        // 인증 불필요 경로
                        // 인증(로그인)이 전혀 필요 없는 경로 -> 비로그인자도 접근 가능
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/concerts/**",
                                "/swagger-ui/**",
                                "/api-docs/**"
                        ).permitAll() // 위의 주소(64~68)들은 로그인 없이 접근 허용(permitAll()) 시킴

                        // 인증(로그인) 필요 경로 -> 인증 안되면 이용 불가능
                        // .authenticated(): 여기 들어오는 것은 무조건 인증(로그인)이 되어야 접속 가능                        .requestMatchers("/api/my/**").authenticated()
                        .requestMatchers("/api/seats/**").authenticated()
                        .requestMatchers("/api/reservations/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()

                        // 적어두지 않은 나머지 모든 API 요청들도 전부 로그인이 필요
                        .anyRequest().authenticated() // 위에 안 적은 자잘한 주소들도 100% 전부 무조건 인증(로그인)을 거쳐야만 통과
                )

                /*
                 *  JWT 인증 필터 등록
                 *
                 * Spring Security 기본 필터: 아이디/비밀번호 검문소(UsernamePasswordAuthenticationFilter)
                 * 원래는 아이디/비번 로그인 처리용 필터
                 *
                 * 현재는 JWT로 인해 아이디/비번 로그인 방식이 아닌 헤더의 JWT 토큰을 검사할 것이 목적
                 *
                 * 요청 흐름
                 * 1. JwtFilter -> 2. JWT검증 -> 3. Authentication 생성 -> 4. SecurityContext 저장 -> Controller 접근 허용
                 */
                .addFilterBefore(
                        new JwtFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class // 기본 필터 클래스의 바로 앞 순서로 배치하겠다는 기준점
                );

        return http.build(); // 위 규칙(37~89라인)대로 조립된 보안 설정 정보들을 최종 빌드하여 성벽 객체로 완성해 리턴
    }

    /*
     * 비밀번호 암호화 인코더 빈(객체) 등록
     *
     * PasswordEncoder: 비밀번호 암호화 전용객체
     * 사용자가 회원가입할 때 입력한 생짜 비밀번호(예: "password123")를 단방향 암호화 처리해 주는 기계.
     *
     * BCrypt: Spring Security에서 가장 많이 사용하는 비밀번호 암호화 알고리즘
     * BCrypt 알고리즘을 사용하며, 실행할 때마다 랜덤한 소금(Salt)을 쳐서 똑같은 비밀번호도 완전히 다른 암호문으로 변환.
     * 서비스 계층에서 유저 가입 시 파라미터 암호화 및 로그인 시 패스워드 대조 작업에 사용.
     *
     * 특징
     * - 단방향 암호화
     * - 같은 비밀번호도 매번 다른 결과 생성
     * - 내부적으로 Salt 자동 적용
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * [CORS 세부 정책 설정 가이드]: 어떤 주소를 허용할지, 어떤 HTTP 메소드를 허용할지 설정
     * 외부(프론트엔드)에서 백엔드 API를 찌를 때 어떤 규칙까지 허용해 줄지 상세하게 작성하는 공간.
     * 다른 도메인(출처)에 가로막혀 통신이 안 되는 현상을 해결하기 위해 구체적인 허용 범위를 명시하는 리모컨 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // CORS 세부 옵션을 설정할 수 있는 자바 설정 상자를 하나 생성
        CorsConfiguration config = new CorsConfiguration();

        // 허용할 프론트엔드 출처(Origin) 주소를 명시. (로컬에서 개발 중인 리액트/넥스트 서버 주소만 전용 허용)
        // 데이터 조회(GET), 등록(POST), 수정(PUT, PATCH), 삭제(DELETE), 예비 요청(OPTIONS)을 허용
        config.setAllowedOrigins(List.of("http://localhost:3000"));

        // 프론트엔드가 백엔드 API에 보낼 수 있는 HTTP 메서드 종류들을 허용(Authorization 헤더에 JWT를 실어 보낼 것이므로 필수)
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        // 어떤 헤더 정보든 상관없이 전부 허용
        // 어떤 HTTP 헤더 데이터를 실어 보내든 상관없이 전부 허용.
        // Authorization 헤더에 Bearer 토큰을 실어 나를 것이므로 이 헤더 프리패스 설정("*")이 반드시 필요
        config.setAllowedHeaders(List.of("*"));

        // 자바스크립트(Axios, Fetch 등) 요청 시 쿠키나 인증 헤더 정보를 주고받을 수 있도록 자격 증명을 전면 허용(true)
        config.setAllowCredentials(true);

        // 위의 크로스 도메인 규칙을 모든 URL 경로("/**")에 일괄 적용하겠다고 선언하고 반환
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}