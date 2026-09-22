package com.infy.entity;

import com.infy.enums.Role;
import com.infy.enums.SecretQuestion;
import com.infy.enums.UserState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Application user. Deliberately not annotated with Lombok's @Data (see
 * plan.md "Tech Stack Additions" -> Lombok usage convention): entities keep
 * an explicit @EqualsAndHashCode(of = "id") instead of an all-fields one, and
 * no @Data-generated toString() that could pull in lazy relations.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserState userState = UserState.ACTIVATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecretQuestion secretQuestion;

    @Column(nullable = false)
    private String secretAnswer;
}
