package juon.querydsl.entity;

import lombok.Data;
import org.springframework.util.ObjectUtils;

@Data
public class MemberDTO {
    private String username;
    private int age;
    private String teamName;

    public MemberDTO(String username, int age, String teamName) {
        this.username = username;
        this.age = age;
        this.teamName = teamName;
    }

    public MemberDTO(Member memb) {
        if (!ObjectUtils.isEmpty(memb)) {
            this.username = memb.getUsername();
            this.age = memb.getAge();

            if (!ObjectUtils.isEmpty(memb.getTeam())) {
                this.teamName = memb.getTeam().getName();
            }
        }
    }
}
