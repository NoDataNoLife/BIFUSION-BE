package com.nodatanolife.bifusion_be.global.security.oauth2;

import com.nodatanolife.bifusion_be.domain.project.service.ProjectInvitationService;
import com.nodatanolife.bifusion_be.domain.user.entity.OauthAccount;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.OauthAccountRepository;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nodatanolife.bifusion_be.domain.user.config.PointProperties;
import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.service.PointService;

import java.util.Map;


@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final OauthAccountRepository oauthAccountRepository;
    private final ProjectInvitationService projectInvitationService;
    private final PointService pointService;
    private final PointProperties pointProperties;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(request);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String provider   = request.getClientRegistration().getRegistrationId();
        String providerId = (String) attributes.get("sub");
        String email      = (String) attributes.get("email");
        String name       = (String) attributes.get("name");

        OauthAccount oauthAccount = oauthAccountRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(email)
                            .orElseGet(() -> createUserWithPendingInvitations(email, name));
                    return oauthAccountRepository.save(
                            OauthAccount.create(user, provider, providerId));
                });

        return new CustomOAuth2User(oauthAccount.getUser(), attributes);
    }

    private User createUserWithPendingInvitations(String email, String name) {
        User newUser = userRepository.save(User.createOAuthUser(email, name));
        pointService.earn(newUser.getUserId(), pointProperties.signupBonus(), RewardHistory.TYPE_SIGNUP_BONUS, null, null);
        projectInvitationService.linkPendingInvitationsTo(newUser);
        return newUser;
    }
}
