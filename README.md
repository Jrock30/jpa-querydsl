# QueryDSL
- - -
- 라이브러리
  > - querydsl-apt: Querydsl 관련 코드 생성 기능 제공
  > - querydsl-jpa: querydsl 라이브러리
  > - spring-boot-starter-web
  > >  - spring-boot-starter-tomcat: 톰캣 (웹서버)
  > >  - spring-webmvc: 스프링 웹 MVC
  >
  > - spring-boot-starter-data-jpa
  > >  - spring-boot-starter-aop
  > >  - spring-boot-starter-jdbc
  > > > HikariCP 커넥션 풀 (부트 2.0 기본)
  > >
  > > - hibernate + JPA: 하이버네이트 + JPA
  > > - spring-data-jpa: 스프링 데이터 JPA
  >
  > - spring-boot-starter(공통): 스프링 부트 + 스프링 코어 + 로깅
  > > - spring-boot
  > > > - spring-core
  > >
  > > - spring-boot-starter-logging
  > > > - logback, slf4j
  >
  > - spring-boot-starter-test
  > > - junit: 테스트 프레임워크, 스프링 부트 2.2부터 junit5( jupiter ) 사용
  > > > - 과거 버전은 vintage
  > >
  > > - mockito: 목 라이브러리
  > > - assertj: 테스트 코드를 좀 더 편하게 작성하게 도와주는 라이브러리
  > > > - https://joel-costigliola.github.io/assertj/index.html
  > >
  > > - spring-test: 스프링 통합 테스트 지원
- Querydsl 설정
- Querydsl 활용 (TEST 예제 코드 활용)
  * 검색조건, 정렬, 페이징, 집합, 조인, 서브쿼리, Case문, 상수/문자 더하기
  * 프로젝션 결과 반환
    * DTO, @QueryProjection
  * 동적쿼리
    * BooleanBuilder
    * where 다중 파라미터
  * 수정, 삭제 벌크 연산
  * SQL Function 호출    
- - -
- 순수 JPA 와 Querydsl (Repository 생성)
  * 순수 JPA Repository, Querydsl
    * 동적 쿼리와 성능 최적화 조회 - Builder 사용
    * 동적 쿼리와 성능 최적화 조회 - Where절 파라미터 사용
    * 조회 API 컨트롤러 개발   
- - -
- Spring Data Jpa 와 Querydsl (Repository 생성)
  * 사용자 정의 리포지토리
  
  

  