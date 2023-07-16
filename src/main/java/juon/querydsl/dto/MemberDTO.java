package juon.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import juon.querydsl.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Kim Juon
 */
@Data
@NoArgsConstructor
public class MemberDTO {
    private String username;
    private int age;

    @QueryProjection
    public MemberDTO(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
