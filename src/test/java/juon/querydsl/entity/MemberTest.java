package juon.querydsl.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;


@SpringBootTest
@Transactional
class MemberTest {
    @PersistenceContext
    EntityManager em;

    @Test
    void initTest() {
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

        em.flush();
        em.clear();

        List<Member> members = em.createQuery("select m from Member m", Member.class).getResultList();
        Assertions.assertThat(members.size()).isEqualTo(4);
    }
}