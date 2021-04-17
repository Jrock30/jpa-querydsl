package com.jrock.querydsl;

import com.jrock.querydsl.entity.Member;
import com.jrock.querydsl.entity.QMember;
import com.jrock.querydsl.entity.QTeam;
import com.jrock.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.jrock.querydsl.entity.QMember.*;
import static com.jrock.querydsl.entity.QTeam.*;
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
//        QMember m = new QMember("m"); // 인자값은 별칭임 (구분을 위한, 크게 중요하지 않음, 다르게 사용함), 같은 테이블을 조인할 때 alias 를 따로 주어서 사용하면 된다.
//        QMember qMember = QMember.member; // 이렇게 스태틱으로 가져올 수 있다.

        Member findMember = queryFactory
//                .select(QMember.member) // 이렇게 써도 되고 아래처럼 스태틱 임포트해서 사용해도 된다.
                .select(member) // 스태틱 임포트 권장
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩 (sql injection 방어를 자동으로 해줌)
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchParam() throws Exception {
        /**
         * where() 에 파라미터로 검색조건을 추가하면 AND 조건이 추가됨
         * 이 경우 null 값은무시 메서드 추출을 활용해서 동적쿼리를 깔끔하게 만들 수 있음
         */
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10) // .and 대신 , 로 끊으면 and 가 붙는다.
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() throws Exception {

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // 이렇게 되면 쿼리가 두번 실행 됨. select 카운터 쿼리, select 쿼리, 페이징 성능이 중요하면 이거 말고 카운터 쿼리 따로 select 쿼리 따로 직접 날리자.
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal(); //
        List<Member> content = results.getResults(); // 이렇게 해야 데이터가 나옴.

        // 카운트 쿼리만 나감.
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() throws Exception {

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() throws Exception {

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() throws Exception {

        QueryResults<Member> result2 = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(result2.getTotal()).isEqualTo(4);
        assertThat(result2.getLimit()).isEqualTo(2);
        assertThat(result2.getOffset()).isEqualTo(1);
        assertThat(result2.getResults().size()).isEqualTo(2);
    }

    /**
     * JPQL
     * JPQL이 제공하는 모든 집합 함수를 제공한다. tuple은 프로젝션과 결과반환에서 설명한다.
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이 * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) // inner join
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {

        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
//                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     *
     * from 절에 여러 엔티티를 선택해서 세타 조인
     * 외부 조인불가능(outer join) -> 조인 on을 사용하면 외부 조인가능
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // 연관관계가 없는 필드로 조인, theta join
                .where(member.username.eq(team.name)) // 모든 회원과 모든 팀을 가져와서 필터링 (물론 디비가 최적화는 함), 세타조인
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     *
     * on 절을 활용해 조인 대상을 필터링 할 때,
     * 외부조인이 아니라 내부조인(inner join)을 사용하면, where 절에서 필터링 하는 것과 기능이 동일하다.
     * 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때,
     * 내부조인 이면 익숙한 where 절로 해결하고, 정말 외부조인이 필요한 경우에만 이 기능을 사용하자.
     */
    @Test
    public void join_on_filtering() throws Exception {

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")) // left outer join
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     *  2. 연관관계 없는 엔티티 외부 조인
     *    예)회원의 이름과 팀의 이름이 같은 대상 외부 조인
     *    JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     *    SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     *
     *  하이버네이트 5.1부터 on 을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가되었다. 물론 내부 조인도 가능하다.
     *  주의! 문법을 잘 봐야 한다. leftJoin() 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
     *  일반조인: leftJoin(member.team, team)
     *  on조인: from(member).leftJoin(team).on(xxx)
     */
    @Test
    public void join_on_no_relation() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name)) // 보통은 leftJoin(member.team, team) 으로 들어가는데 이와 같이 하면
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 조인 - 페치 조인
     * 페치 조인은 SQL에서 제공하는 기능은 아니다. SQL조인을 활용해서 연관된 엔티티를 SQL 한번에
     * 조회하는 기능이다. 주로 성능 최적화에 사용하는 방법이다.
     */

    /**
     * 페치조인 미적용
     * 지연로딩으로 Member, Team SQL 쿼리 각각 실행
     */
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        // 페치조인을 테스트 할 때는 영속성 컨텍스를 확실히 비워주고 시작하는 것이 좋다.
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).as("페치 조인 미적용.").isFalse();
    }

    /**
     * 페치 조인 적용
     * 즉시로딩으로 Member, Team SQL 쿼리 조인으로 한번에 조회
     */
    @Test
    public void fetchJoinUse() throws Exception {
        // 페치조인을 테스트 할 때는 영속성 컨텍스를 확실히 비워주고 시작하는 것이 좋다.
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).as("페치 조인 적용.").isTrue();
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
