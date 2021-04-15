package com.jrock.querydsl;

import com.jrock.querydsl.entity.Member;
import com.jrock.querydsl.entity.QMember;
import com.jrock.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @Test
    public void startJPQL() {
        String qlString =
                "select m from Member m" +
                " where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    /**
     * EntityManager 로 JPAQueryFactory 생성
     * Querydsl은 JPQL 빌더
     * JPQL: 문자(실행 시점 오류), Querydsl: 코드(컴파일 시점 오류)
     * JPQL: 파라미터 바인딩 직접,  Querydsl: 파라미터 바인딩 자동 처리
     *
     * JPAQueryFactory를 필드로 제공하면 동시성 문제는 어떻게 될까?
     * 동시성 문제는 JPAQueryFactory를 생성할 때 제공하는 EntityManager(em)에 달려있다.
     * 스프링 프레임워크는 여러 쓰레드에서 동시에 같은 EntityManager에 접근해도,
     * 트랜잭션 마다 별도의 영속성 컨텍스트를 제공하기 때문에, 동시성 문제는 걱정하지 않아도 된다.
     */
    @Test
    public void startQuerydsl() {
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em); // 하나만 생성해서 모든 곳에 공유해도 되므로, @Bean 등록이나, 리포지토리 필드 레벨로 지정해도 무방. 동시성 문제 고민 안해도 됨.
        QMember m = new QMember("m"); // 인자값은 별칭임 (구분을 위한, 크게 중요하지 않음, 다르게 사용함)

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) // 파라미터 바인딩 (sql injection 방어를 자동으로 해줌)
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em); // 필드 레벨에 적용, 동시성 문제 고민 안해도 됨.

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }
}
