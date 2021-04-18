package com.jrock.querydsl.repository;

import com.jrock.querydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    /**
     * 기존 MemberJpaRepository 에 있는 메서드 들은 Spring Data Jpa 에서 자동으로 생성해주므로 다 제거가 가능
     */

    // select m from Member m where m.username = ?
    List<Member> findByUsername(String username);

}
