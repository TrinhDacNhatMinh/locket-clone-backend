package com.minh.locket_clone_backend.friend.entity;

import com.minh.locket_clone_backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "friends",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_a_id", "user_b_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friend extends BaseEntity {

    // Convention: userAId is always less than userBId (UUID comparison)
    @Column(name = "user_a_id", nullable = false)
    private UUID userAId;

    @Column(name = "user_b_id", nullable = false)
    private UUID userBId;

    @PrePersist
    @PreUpdate
    public void autoSwapToEnsureOrder() {
        if (userAId != null && userBId != null && userAId.compareTo(userBId) > 0) {
            UUID temp = userAId;
            this.userAId = userBId;
            this.userBId = temp;
        }
    }
}
