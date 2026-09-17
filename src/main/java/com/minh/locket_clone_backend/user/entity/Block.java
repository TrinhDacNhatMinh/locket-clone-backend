package com.minh.locket_clone_backend.user.entity;

import com.minh.locket_clone_backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "blocks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Block extends BaseEntity {

    @Column(name = "blocker_id", nullable = false)
    private UUID blockerId;

    @Column(name = "blocked_id", nullable = false)
    private UUID blockedId;
}
