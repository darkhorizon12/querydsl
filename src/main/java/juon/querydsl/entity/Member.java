package juon.querydsl.entity;

import lombok.*;
import org.springframework.util.ObjectUtils;

import javax.persistence.*;

@Entity
@Setter @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"team"})
public class Member extends BaseEntity {
    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;

    private int age;

    public Member(String username, int age) {
        this.username = username;
        this.age = age;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // foreign key column name
    private Team team;

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (!ObjectUtils.isEmpty(team)) {
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
