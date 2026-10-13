package com.newlife.hospital.security;

import com.newlife.hospital.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request from the session cookie. The user row is re-checked on every request, so disabling an
 * account, deleting it, changing its role or bumping its token version takes effect immediately.
 * Not a Spring bean on purpose: it is only registered inside the security filter chain.
 */
public class JwtCookieAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthCookieService cookieService;
    private final UserRepository userRepository;

    public JwtCookieAuthFilter(JwtService jwtService, AuthCookieService cookieService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        cookieService.read(request)
                .flatMap(jwtService::verify)
                .flatMap(claims -> userRepository.findByUsername(claims.username())
                        .filter(user -> user.isEnabled()
                                && user.getTokenVersion() == claims.tokenVersion()
                                && user.getRole().name().equals(claims.role())))
                .ifPresentOrElse(user -> {
                    AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole());
                    var auth = new UsernamePasswordAuthenticationToken(principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);
                }, () -> {
                    // A stale or invalid cookie is removed so the browser stops sending it.
                    if (cookieService.read(request).isPresent()) {
                        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.clearedCookie().toString());
                    }
                });
        chain.doFilter(request, response);
    }
}
