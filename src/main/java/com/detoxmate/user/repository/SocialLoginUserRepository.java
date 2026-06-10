package com.detoxmate.user.repository;

import com.detoxmate.user.domain.SocialLoginUser;
import com.detoxmate.user.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialLoginUserRepository extends JpaRepository<SocialLoginUser, Long> {

    Optional<SocialLoginUser> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    List<SocialLoginUser> findAllByUserId(Long userId);

    void deleteByUserId(Long userId);
}
