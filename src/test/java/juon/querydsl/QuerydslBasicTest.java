package juon.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import juon.querydsl.entity.Member;
import juon.querydsl.entity.QMember;
import juon.querydsl.entity.QTeam;
import juon.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

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
}
