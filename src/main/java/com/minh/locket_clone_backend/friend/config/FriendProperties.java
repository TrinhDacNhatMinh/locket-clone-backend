package com.minh.locket_clone_backend.friend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class FriendProperties {

    private final int maxLimit;

    public FriendProperties(@Value("${app.friend.max-limit:20}") int maxLimit) {
        this.maxLimit = maxLimit;
    }
}
