package com.minh.locket_clone_backend.reaction.entity;

import com.minh.locket_clone_backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "reactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reaction extends BaseEntity {

    @Column(name = "photo_id", nullable = false)
    private UUID photoId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "emoji", nullable = false, length = 10)
    private String emoji;

    @Column(name = "seen", nullable = false)
    @Builder.Default
    private boolean seen = false;
}
