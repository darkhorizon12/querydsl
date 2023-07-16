package juon.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import juon.querydsl.dto.MemberDTO;
import juon.querydsl.dto.QMemberDTO;
import juon.querydsl.dto.UserDTO;
import juon.querydsl.entity.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.criteria.From;

import java.util.List;

import static juon.querydsl.entity.QMember.member;
import static juon.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @PersistenceContext EntityManager em;
    @Autowired JPAQueryFactory factory;

    @BeforeEach
    void init() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member memb1 = new Member("memb1", 31, teamA);
        Member memb2 = new Member("memb2", 32, teamA);
        Member memb3 = new Member("memb3", 33, teamB);
        Member memb4 = new Member("memb4", 34, teamB);
        em.persist(memb1);
        em.persist(memb2);
        em.persist(memb3);
        em.persist(memb4);
    }

    @Test
    void startJPQL() {
        String username = "memb1";
        Member result = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getSingleResult();

        assertThat(result.getUsername()).isEqualTo(username);
    }

    @Test
    void startQuerydsl() {
        String username = "memb1";

        Member result = factory.select(member)
                .from(member)
                .where(member.username.eq(username))
                .fetchOne();

        assertThat(result.getUsername()).isEqualTo(username);
    }

    @Test
    void searchParam() {
        Member memb1 = factory.selectFrom(member)
                .where(
                        member.username.eq("memb1")
                                .and(member.age.eq(31))
                )
                .fetchOne();
        assertThat(memb1).isNotNull();
    }

    @Test
    void searchAndParam() {
        Member memb1 = factory.selectFrom(member)
                .where(
                        member.username.eq("memb1"),
                        member.age.eq(31)
                )
                .fetchOne();
        assertThat(memb1).isNotNull();
    }

    @Test
    void resultFetch() {
//        Member member1 = factory.selectFrom(member)
//                .limit(1)
//                .orderBy(member.age.asc())
//                .fetchOne();
//
//        Member member2 = factory.selectFrom(member)
//                .fetchFirst();
//        assertThat(member1.getAge()).isEqualTo(31);
//        assertThat(member2.getAge()).isEqualTo(31);

        QueryResults<Member> queryResults = factory
                .selectFrom(member)
                .fetchResults();
        System.out.println("queryResults.getOffset() = " + queryResults.getOffset());
    }

    @Test
    void sort() {
        // 정렬순서
        // 나이 내림차
        // 이름 올림차
        // 이름이 없으면 마지막에 출력
        Member memb0 = new Member(null, 100);
        em.persist(memb0);
        Member memb5 = new Member("memb5", 100);
        em.persist(memb5);
        Member memb6 = new Member("memb6", 100);
        em.persist(memb6);

        List<Member> sortedList = factory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertThat(sortedList.get(0)).isEqualTo(memb5);
        assertThat(sortedList.get(sortedList.size() - 1)).isEqualTo(memb0);
    }

    @Test
    void paging() {
        List<Member> fetch = factory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(fetch.size()).isEqualTo(2);
    }

    @Test
    void aggregation() {
        Tuple tuple = factory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetchOne();

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(130);
        assertThat(tuple.get(member.age.max())).isEqualTo(34);
    }

    @Test
    void group() {
        List<Tuple> fetch = factory
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team(), team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = fetch.get(0);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
    }

    @Test
    void join() {
        List<Member> fetch = factory
                .selectFrom(member)
                .join(member.team(), team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(fetch.size()).isEqualTo(2);
        assertThat(fetch)
                .extracting("username")
                .containsExactly("memb1", "memb2");
    }

    @Test
    void join_on_filtering() {
        List<Tuple> fetch = factory
                .select(member, team)
                .from(member)
                .join(member.team(), team).on(team.name.eq("teamA"))
                .fetch();

        assertThat(fetch.size()).isEqualTo(2);
    }

    @Test
    void join_on_no_relation() {
        em.persist(new Member("teamA", 10));
        em.persist(new Member("teamB", 20));

        List<Tuple> fetch = factory
                .select(member, team)
                .from(member)
                .join(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void no_fetch_join() {
        em.flush();
        em.clear();

        Member memb1 = factory
                .selectFrom(member)
                .where(member.username.eq("memb1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(memb1.getTeam());

        assertThat(loaded).isFalse();
    }

    @Test
    void fetch_join() {
        em.flush();
        em.clear();

        Member memb1 = factory
                .selectFrom(member)
                .join(member.team(), team).fetchJoin()
                .where(member.username.eq("memb1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(memb1.getTeam());

        assertThat(loaded).isTrue();
    }

    @Test
    void sub_query() {
        QMember subMemb = new QMember("subMemb");

        Member member1 = factory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(subMemb.age.max())
                                .from(subMemb)
                ))
                .fetchOne();

        assertThat(member1.getAge()).isEqualTo(34);

        List<Member> fetch = factory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(subMemb.age.avg())
                                .from(subMemb)

                ))
                .fetch();

        assertThat(fetch)
                .extracting("age")
                .containsExactly(33, 34);
    }

    @Test
    void subQueryIn() {
        QMember subMemb = new QMember("subMemb");

        List<Member> fetch = factory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(subMemb.age)
                                .from(subMemb)
                                .where(subMemb.age.gt(31))
                ))
                .fetch();

        assertThat(fetch).extracting("age")
                .containsExactly(32, 33, 34);
    }

    @Test
    void selectSubQuery() {
        QMember subMemb = new QMember("subMemb");

        List<Tuple> fetch = factory
                .select(member.username,
                        JPAExpressions
                                .select(subMemb.age.avg())
                                .from(subMemb)
                )
                .from(member)
                .fetch();

        assertThat(fetch.size()).isEqualTo(4);
    }

    @Test
    void basicCase() {
        List<String> fetch = factory
                .select(member.age
                        .when(31).then("31살")
                        .when(32).then("32살")
                        .otherwise(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    void complexCase() {
        List<String> fetch = factory
                .select(new CaseBuilder()
                        .when(member.age.between(31, 32)).then("31 ~ 32살")
                        .when(member.age.between(33, 34)).then("33 ~ 34살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void concat() {
        List<String> result = factory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("memb1"))
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    void tupleProjection() {
        List<Tuple> tuples = factory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        
        tuples.forEach(tuple -> {
            System.out.println("tuple.get(member.username) = " + tuple.get(member.username));
            System.out.println("tuple.get(member.age) = " + tuple.get(member.age));
        });
    }

    @Test
    void findDtoByJPQL() {
        List<MemberDTO> res = em.createQuery("select new juon.querydsl.dto.MemberDTO(m.username, m.age) from Member m", juon.querydsl.dto.MemberDTO.class)
                .getResultList();

        res.forEach(System.out::println);
    }

    /**
     * setter 를 통해 인젝션
     */
    @Test
    void findDtoBySetter() {
        List<MemberDTO> res = factory
                .select(
                        Projections.bean(MemberDTO.class,
                                member.username,
                                member.age)
                )
                .from(member)
                .fetch();

        res.forEach(System.out::println);
    }

    /**
     * field에 바로 인젝션(setter 필요없음)
     */
    @Test
    void findDtoByField() {
        List<MemberDTO> res = factory
                .select(
                        Projections.fields(MemberDTO.class,
                                member.username,
                                member.age)
                )
                .from(member)
                .fetch();

        res.forEach(System.out::println);
    }

    /**
     * field에 바로 인젝션(setter 필요없음)
     */
    @Test
    void findDtoByConstructor() {
        List<MemberDTO> res = factory
                .select(
                        Projections.constructor(MemberDTO.class,
                                member.username,
                                member.age)
                )
                .from(member)
                .fetch();

        res.forEach(System.out::println);
    }

    @Test
    void findOtherDTO() {
        QMember membSub = new QMember("membSub");
        List<UserDTO> fetch = factory
                .select(Projections.fields(
                                UserDTO.class,
                                member.username.as("name"),

                                ExpressionUtils.as(
                                        JPAExpressions
                                                .select(membSub.age.max())
                                                .from(membSub), "age")
                        )
                )
                .from(member)
                .fetch();

        for (UserDTO userDTO : fetch) {
            System.out.println("userDTO = " + userDTO);
        }
    }

    /**
     * QueryProjection은 dto를 queryDsl에 종속시켜서
     * controller, service layer에서는 사용하기가 힘들다는 단점이 있다.
     * querydsl을 사용하지 않을 경우, controller, service까지 수정해야 하기 때문에
     */
    @Test
    void findDtoByQueryProjection() {
        List<MemberDTO> res = factory
                .select(new QMemberDTO(member.username, member.age))
                .from(member)
                .fetch();

        res.forEach(System.out::println);
    }

    @Test
    void dynamicQuery_booleanBuilder() {
        String usernameParam = "memb1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    @Test
    void dynamicQuery_whereParam() {
        String usernameParam = "memb1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {
        BooleanBuilder builder = new BooleanBuilder();
        if (! ObjectUtils.isEmpty(usernameParam)) {
            builder.and(member.username.eq(usernameParam));
        }
        if (! ObjectUtils.isEmpty(ageParam)) {
            builder.and(member.age.eq(ageParam));
        }
        return factory
                .selectFrom(member)
                .where( builder )
                .fetch();
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return factory
                .selectFrom(member)
                .where(allEq(usernameParam, ageParam))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return ObjectUtils.isEmpty(usernameCond)
                ? null
                : member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ObjectUtils.isEmpty(ageCond)
                ? null
                : member.age.eq(ageCond);
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * bulk 연산은 영속성컨텍스트를 무시하고 DB에 직접 실행쿼리를 날린다.
     * 따라서 조회쿼리를 하게 되면 업데이트를 위한 엔티티들이 영속성 컨텍스트에 있기 때문에
     * 조회쿼리의 내용과 실제 DB 내용이 맞지 않는다.
     * 따라서 벌크연산 이후 DB와 영속성컨텍스트를 동기화하기 위해서는
     * 영속성 컨텍스트를 초기화해야 한다.
     */
    @Test
    void bulkUpdates() {
        long count = factory
                .update(member)
                .set(member.username, "non_memb")
                .where(member.age.lt(33))
                .execute();

        Assertions.assertThat(count).isEqualTo(2);

        em.flush();
        em.clear();

        List<Member> fetch = factory
                .selectFrom(member)
                .where(member.age.lt(33))
                .fetch();

        Assertions.assertThat(fetch.get(0).getUsername()).isEqualTo("non_memb");

        factory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        em.flush();
        em.clear();

        factory
                .selectFrom(member)
                .fetch()
                .stream()
                .forEach(System.out::println);
    }
}
