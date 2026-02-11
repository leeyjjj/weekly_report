package com.weekly.security;

import com.weekly.entity.User;
import com.weekly.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.sso.enabled", havingValue = "true")
public class CustomOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    private final UserRepository userRepository;

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String sub = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        if (name == null || name.isBlank()) {
            name = oidcUser.getPreferredUsername();
        }

        log.info("OIDC login: sub={}, email={}, name={}", sub, email, name);

        User user = resolveUser(sub, email, name);
        log.info("Mapped to local user: id={}, name={}", user.getId(), user.getName());

        return oidcUser;
    }

    private User resolveUser(String sub, String email, String name) {
        // 1) oauthId로 매칭
        Optional<User> byOauth = userRepository.findByOauthId(sub);
        if (byOauth.isPresent()) {
            return byOauth.get();
        }

        // 2) email로 매칭
        if (email != null && !email.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                user.setOauthId(sub);
                return userRepository.save(user);
            }
        }

        // 3) name으로 매칭
        if (name != null && !name.isBlank()) {
            Optional<User> byName = userRepository.findByName(name);
            if (byName.isPresent()) {
                User user = byName.get();
                user.setOauthId(sub);
                return userRepository.save(user);
            }
        }

        // 4) 신규 생성
        User newUser = new User();
        newUser.setName(name != null && !name.isBlank() ? name : email);
        newUser.setEmail(email);
        newUser.setOauthId(sub);
        return userRepository.save(newUser);
    }
}
