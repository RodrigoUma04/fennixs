package com.fennixs.coreapi.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.fennixs.coreapi.persistence.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    private String issuer;

    private String subject;

    @Setter
    private String email;

    private boolean enabled = true;

    @Builder(access = AccessLevel.PACKAGE)
    User(String issuer, String subject, String email) {
        this.issuer = issuer;
        this.subject = subject;
        this.email = email;
    }
}
