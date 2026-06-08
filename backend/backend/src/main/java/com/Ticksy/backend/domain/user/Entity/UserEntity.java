package com.Ticksy.backend.domain.user.Entity;

import com.Ticksy.backend.domain.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
/*
* @NoArgsConstructor(access = AccessLevel.PROTECTED)
* 매개변수가 없는 기본 생성자를 protected 접근 제어자로 설정하여 생성
* protected UserEnttiy() { }
*
* 사용 이유
* - JPA는 객체 생성을 해야 하지만, 개발자가 마음대로 생성하는 것은 막고 싶기 때문이다.
* - UserEntity user = userRepository.findById(1L); ==  UserEntity user = new UserEntity(); 실제 내부에서 비슷하게 동작
* -JPA는 엔티티 조회 시 내부적으로 기본 생성자를 사용하여 객체를 생성한다.
* - 그 후 Reflection으로 필드 값 주입 -> JPA는 기본 생성자 필요, 매개변수 없는 생성자 필요
* - 프록시 객체의 생성과 객체에 대한 접근 범위 문제를 해결하기 위해서 사용
*
* - public이 아닌 protected 쓰는 이유
* - public 사용시 어디서든 객체 생성이 가능하다. -> 문제는 객체가 이상한 상태로 생성가능
* ㄴ 객체 무결성 깨짐, 비즈니스 규칙 붕괴, 실수 가능성 증가
*
* - 따라서 protected 사용하여 외부 패키지에서 직접 생성 제한을 하는게 좋다
* - 하지만 JPA 내부, 상속 클래스, 같은 패키지에서 접근 가능
* - 객체 생성 제한과 JPA 요구사항을 만족할 수 있다.
* */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // TODO: 33 ~ 37 여유가 생길 시 소셜 로그인 구현 때 사용
    // @Column(name = "provider", nullable = true, length = 20)
    // private String provider;

    // @Column(name = "provider_id", nullable = true, length = 100)
    // private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isDeleted;

    // 소프트 삭제
    public void delete() {
        this.isDeleted = true;
    }

    // 비밀번호 변경
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /*
     * 상태 변경 메소드
     * 엔티티 자신의 상태를 스스로 관리하기 위해서이 만듬
     * 즉, 단순히 아무데서나 값을 막 바꾸는게 아니다.
     * ㄴ 어떤값이 변경이 되는가?
     * ㄴ 어떤 규칙으로 변경되는가?
     * ㄴ 변경 시 검증이 필요하기 때문이다.
     *
     * 상태 메소드 사용 시
     * - 행위가 명확하다.
     * - 비즈니스 로직 캡슐화 가능 -> 외부에 노출 안됨
     * - 객체 상태 통제 가능하다
     *
     * 세터를 사용시
     * ㄴ 어디서든 수정이 가능하다.
     * ㄴ어떤 상황인지 의미가 불명확하다.
     * ㄴ검증 로직이 흩어진다.
     * ㄴ객체 무결성이 깨지기 쉽다.
     */
}
