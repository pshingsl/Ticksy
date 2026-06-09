package com.Ticksy.backend.domain.user.Repository;

import com.Ticksy.backend.domain.user.Entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 이메일로 회원 조회(로그인, 중복 확인)
    // Select * from users where email = ?
    Optional<UserEntity> findByEmail(String email);

    // 이메일 존재 여부(중복 확인)
    // Optional<T> 사용 하지 않는 이유 JPA에서 제공하는 existsBy에서는 있냐, 없냐 두 가지만 알면 끝나기에 필요없
    // Select * FROM users WHERE EXISTS(Select * FROM users where email = )
    boolean existsByEmail(String email);

    // 이메일로 조회(탈퇴하지 않은 회원만)
    // select * from users where email = ? and is_deleted = false
    // is_deleted = false 되는 이유는 우리가 만든 엔티티 클래스의 쿼리가 매핑이 되기 떄문이다.
    Optional<UserEntity> findByEmailAndIsDeletedFalse(String email);
}
