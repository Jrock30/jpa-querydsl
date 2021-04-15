package com.jrock.querydsl.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 롬복 설명
 *   - @Setter: 실무에서 가급적 Setter는 사용하지 않기
 *   - @NoArgsConstructor AccessLevel.PROTECTED: 기본 생성자 막고 싶은데, JPA 스팩상 PROTECTED로 열어두어야 함
 *   - @ToString은 가급적 내부 필드만(연관관계 없는 필드만)
 * changeTeam() 으로 양방향 연관관계 한번에 처리(연관관계 편의 메소드)
 */
@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "name"})
public class Team {
    @Id @GeneratedValue
    @Column(name = "team_id")
    private Long id;
    private String name;

    @OneToMany(mappedBy = "team")
    List<Member> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }
}