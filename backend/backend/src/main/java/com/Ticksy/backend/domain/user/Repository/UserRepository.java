package com.Ticksy.backend.domain.user.Repository;

import com.Ticksy.backend.domain.user.Entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 이메일로 회원 조회(로그인, 중복 확인)
    Optional<UserEntity> findByEmail(String email);

    // 이메일 존재 여부(중복 확인)
    boolean existsByEmail(String email);

    // 이메일로 조회(탈퇴하지 않은 회원만)
    Optional<UserEntity> findByEmailAndIsDeletedFalse(String email);
}
