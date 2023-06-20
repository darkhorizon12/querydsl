package juon.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import juon.querydsl.entity.Hello;
import juon.querydsl.entity.QHello;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {
    @PersistenceContext
    EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory factory = new JPAQueryFactory(em);
        QHello qhello = QHello.hello;

        Hello result = factory.selectFrom(qhello)
                .fetchOne();

        Assertions.assertThat(result).isEqualTo(hello);
    }

}
