package com.jrock.querydsl;

import com.jrock.querydsl.dto.MemberDto;
import com.jrock.querydsl.dto.QMemberDto;
import com.jrock.querydsl.dto.UserDto;
import com.jrock.querydsl.entity.Member;
import com.jrock.querydsl.entity.QMember;
import com.jrock.querydsl.entity.QTeam;
import com.jrock.querydsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
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
import java.util.function.Supplier;

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

    /**
     * 서브쿼리
     *
     *  - from 절의 서브쿼리 한계
     *    - JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
     *    - 당연히 Querydsl 도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다.
     *    - Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.
     *
     *   - from 절의 서브쿼리 해결방안
     *     - 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
     *     - 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     *     - 3. nativeSQL을 사용한다.
     */

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30,40);
    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30,40);
    }

    @Test
    public void selectSubQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions // 스태틱 메소드로 임포트 가능
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * Case 문
     */
    @Test
    public void basicCase() throws Exception {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() throws Exception {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void orderByCase() throws Exception {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);

            System.out.println("username = " + username);
            System.out.println("age = " + age);
            System.out.println("rank = " + rank);
        }
    }

    /**
     * 상수
     *
     * 아래와 같이 최적화가 가능하면 SQL에 constant 값을 넘기지 않는다.
     * 상수를 더하는 것 처럼 최적화가 어려우면 SQL에 constant 값을 넘긴다.
     */
    @Test
    public void constant() throws Exception {

        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 문자 더하기
     *
     * member.age.stringValue() 부분이 중요한데, 문자가 아닌 다른 타입들은 stringValue() 로
     * 문자로 변환할 수 있다. 이 방법은 ENUM을 처리할 때도 자주 사용한다.
     */
    @Test
    public void concat() throws Exception {
        // {username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     *  프로젝션과 결과 반환
     *
     *  프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있음
     *  프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
     */

    /**
     * 프로젝션 대상이 하나
     */
    @Test
    public void simpleProjections() throws Exception {
//        List<Member> result = queryFactory -> 엔티티를 조회 해도 프로젝션 대상이 하나라고 보면 된다
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 튜플 프로젝션
     *
     * 프로젝션 대상이 둘 이상일 때 사용
     * 튜플은 repository 계층에서 사용하는 것은 괜찮은데,
     * 서비스등 다른 계층에서 사용하는 것은 좋지 않다. 핵심 비즈니스 로직은 리포지터리에만 의존하게끔.
     * DTO 로 바꾸어서 반환하는 것을 추천.
     */
    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * 순수 JPA에서 DTO 조회 코드
     *
     * 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
     * DTO의 package이름을 다 적어줘야해서 지저분함
     * 생성자 방식만 지원함
     */
    @Test
    public void findDtoByJPQL() throws Exception {
        // new operation
        List<MemberDto> result = em.createQuery("select new com.jrock.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl 빈 생성(Bean population)
     *
     *   - 프로퍼티 접근 (setter)
     *   - 필드 직접 접근
     *   - 접근 생성자 사용
     */
    // 프로퍼티 접근(setter)
    @Test
    public void findDtoBySetter() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,  // MemberDto 기본 생성자를 만들어 주어야 한다.
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 필드 직접 접근
    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,  // MemberDto 기본 생성자를 만들어 주어야 한다.
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 생성자 접근 ( 생성자 타입이 입력값과 맞아야 한다. )
    @Test
    public void findDtoByConstructor() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,  // MemberDto 기본 생성자를 만들어 주어야 한다.
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 별칭이 다를 때
     *
     * 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
     * ExpressionUtils.as(source,alias) : 필드나, 서브 쿼리에 별칭 사용
     *
     * 참고
     * 1. DTO로 직접 조회하는 이유는 엔티티를 무시하고, 조회용 모델을 바로 만드는 것이 목표. 따라서 중간에 번거롭게 엔티티를 만들 이유가 없다.
     * 2. 이것은 설계상에 큰 문제는 없지만, 성능에서 차이가 난다.
     * 결국 리포지토리 계층에서 DTO를 바로 조회할 때는 엔티티를 거치지 않는 것이 더 나은 방법이다.
     * 다만 리포지토리 계층에서 엔티티를 조회하고, 그 엔티티를 어디선가 DTO로 변환할 때는 이미 엔티티를 조회한 상황이기 때문에 이때는 DTO의 생성자 파라미터를 활용하시는 방법도 괜찮다..
     */
    @Test
    public void findUserDto() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,  // MemberDto 기본 생성자를 만들어 주어야 한다.
                        member.username.as("name"), // 필드에 별칭 적용
//                        ExpressionUtils.as(member.username, "name"), // 위와 동일
                        ExpressionUtils.as( // 서브쿼리 알리야스 지정할 때 사용 (서브쿼리,서브쿼리 알리야스)
                                JPAExpressions // 서브쿼리
                                    .select(memberSub.age.max())
                                    .from(memberSub), "age")
                        ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * 생성자 + @QueryProjection
     *
     * 이 방법은 컴파일러로 타입을 체크할 수 있으므로 가장 안전한 방법이다.
     * 다만 DTO에 QueryDSL 어노테이션을 유지해야 하는 점과 DTO까지 Q 파일을 생성해야 하는 단점이 있다.
     * 아래 QMemberDto 생성자에 없는 데이터를 넣으면 컴파일 에러 갈생
     */
    @Test
    public void findDtoByQueryProjection() throws Exception {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 동적 쿼리 - BooleanBuilder 사용
     */
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
//        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond)); // 이렇게 초기 값을 넣을 수 있다. (앞에 미리 방어 코드를 넣었으면 이렇게 넣어도 좋다)

        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 동적 쿼리 - Where 다중 파라미터 사용
     *
     * where 조건에 null 값은 무시된다.
     * 메서드를 다른 쿼리에서도 재활용 할 수 있다.
     * 쿼리 자체의 가독성이 높아진다.
     */
    @Test
    public void dynamicQuery_WhereParam() throws Exception {
//        String usernameParam = "member1";
        String usernameParam = null;
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);

//        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameCond), ageEq(ageCond)) // null 이 들어오면 무시된다.
                .where(allEq2(usernameCond, ageCond)) // 첫번째 null 만 주의하도록 하자.
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null; // // 간단하면 삼항연산자로

//        if (usernameCond == null) {
//            return null;
//        } else {
//            return member.username.eq(usernameCond);
//        }
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null; // // 간단하면 삼항연산자로
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        // 이렇게 조합하면 편하긴 하나 null 처리는 따로 해주어야 한다.(테스트 해보니 첫번째 것만 널이 나오지 않으면 된다. where 1=1 느낌)
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * BooleanExpression allEq() 와 같이 반환하면 null 이 반환되면 체이닝이 .and() 에러 발생 할 수 있다.
     * 아래와 같이 BooleanBuilder 를 리턴하여 null 을 무시하게끔 사용할 수 있다.
     */
    private BooleanBuilder allEq2(String usernameCond, Integer ageCond) {
        return usernameEq2(usernameCond).and(ageEq2(ageCond));
    }

    private BooleanBuilder ageEq2(Integer age) {
        return nullSafeBuilder(() -> member.age.eq(age));
    }

    private BooleanBuilder usernameEq2(String username) {
        return nullSafeBuilder(() -> member.username.eq(username));
    }

    // 공통 유틸로 만들 경우 public 으로 해서 제공
    public static BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> f) {
        try {
            return new BooleanBuilder(f.get());
        } catch (IllegalArgumentException e) {
            return new BooleanBuilder();
        }
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
