package com.newlife.hospital.web;

import com.newlife.hospital.security.AuthCookieService;
import com.newlife.hospital.security.AuthenticatedUser;
import com.newlife.hospital.service.AuthService;
import com.newlife.hospital.service.AuthService.LoginResult;
import com.newlife.hospital.web.dto.AuthDtos.ChangePasswordRequest;
import com.newlife.hospital.web.dto.AuthDtos.LoginRequest;
import com.newlife.hospital.web.dto.AuthDtos.SessionUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService cookies;

    public AuthController(AuthService authService, AuthCookieService cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<SessionUser> login(@RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookies.sessionCookie(result.token().value(), result.token().expiresAt()).toString())
                .body(result.user());
    }

    /** Returns the signed-in user, or 204 when anonymous (lets the SPA restore sessions without 401 noise). */
    @GetMapping("/me")
    public ResponseEntity<SessionUser> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return user == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(authService.me(user));
    }

    /** Issues the XSRF-TOKEN cookie (done by the CSRF filter on any request). */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user != null) {
            authService.logout(user);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearedCookie().toString())
                .build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<SessionUser> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @RequestBody ChangePasswordRequest request) {
        LoginResult result = authService.changePassword(user, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookies.sessionCookie(result.token().value(), result.token().expiresAt()).toString())
                .body(result.user());
    }
}
