package com.compass.domain.user.entity;

import com.compass.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP, status = 'DELETED' WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Builder
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String nickname;

    @Column(length = 500, name = "profile_image_url")
    private String profileImageUrl;

    @Column(length = 50, name = "travel_style")
    private String travelStyle;

    @Column(length = 50, name = "budget_level")
    private String budgetLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() { return this.status != UserStatus.DELETED; }

    @Override
    public boolean isAccountNonLocked() { return this.status != UserStatus.INACTIVE; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return this.status == UserStatus.ACTIVE; }
}