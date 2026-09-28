package com.minh.locket_clone_backend.chat.entity;

import com.minh.locket_clone_backend.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a conversation "container" between two users.
 * No domain-specific fields needed beyond BaseEntity (id, createdAt, updatedAt).
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation extends BaseEntity {
}
