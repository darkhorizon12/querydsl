package juon.querydsl.entity;

import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public abstract class JpaBaseEntity {
    @Column(updatable = false)  // 속성을 변경해도 update 쿼리 적용되지 않음
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdDate = now;
        this.lastModifiedDate = now;
    }

    @PreUpdate
    private void preUpdate() {
        LocalDateTime now = LocalDateTime.now();
        this.lastModifiedDate = now;
    }
}
