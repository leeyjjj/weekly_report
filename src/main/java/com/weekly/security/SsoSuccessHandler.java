package com.weekly.security;

import com.weekly.entity.User;
import com.weekly.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.sso.enabled", havingValue = "true")
public class SsoSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(SsoSuccessHandler.class);

    private final UserRepository userRepository;

    public SsoSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String sub = oidcUser.getSubject();
            Optional<User> user = userRepository.findByOauthId(sub);
            if (user.isPresent()) {
                request.getSession().setAttribute("currentUserId", user.get().getId());
                log.info("Session currentUserId set to {} for user '{}'",
                        user.get().getId(), user.get().getName());
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
