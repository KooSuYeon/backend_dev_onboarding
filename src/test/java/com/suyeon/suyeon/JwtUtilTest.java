package com.suyeon.suyeon;

import com.suyeon.suyeon.config.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtUtilTest {

    private static JwtUtil jwtUtil;
    private static SecretKey secretKey;

    @Mock private HttpServletRequest request;

    @BeforeAll
    public static void setUp() {
        String secret = "cef2660faf36986dca4d1c4b5850eaf0be4900af9713f11a9ef86c952eb53d0c";
        secretKey =
                new SecretKeySpec(
                        secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
        jwtUtil = new JwtUtil(secret);
    }

    @Test
    public void createAccessJwt_ShouldContainClaims_WhenAccessTokenIsCreated()
    {
        String username = "testMember";
        String type = "access";
        String issuer = "suyeon";
        Long duration = 1000L * 60 * 60;

        String accessToken = jwtUtil.createJwt(username, type, issuer, duration);
        assertNotNull(accessToken);

        Claims claims =
                Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(accessToken).getPayload();

        assertEquals(type, claims.get("type"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    public void createRefreshJwt_ShouldContainClaims_WhenRefreshTokenIsCreated()
    {
        String username = "testMember";
        String type = "refresh";
        String issuer = "suyeon";
        Long duration = 1000L * 60 * 60;

        String refreshToken = jwtUtil.createJwt(username, type, issuer, duration);
        assertNotNull(refreshToken);

        Claims claims =
                Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(refreshToken).getPayload();

        assertEquals(type, claims.get("type"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    public void getIsExpiration_ShouldExpiredJwtException_WhenTokenExpired() throws InterruptedException {
        String expiredToken = jwtUtil.createJwt("testMember", "access", "suyeon", 1L);
        TimeUnit.MICROSECONDS.sleep(1500);

        assertThrows(ExpiredJwtException.class, () -> jwtUtil.validateToken(expiredToken));
    }

        @Test
    public void getUsername_ShouldReturnUsername_WhenTokenPassed()
    {
        String username = "testMember";
        String token = jwtUtil.createJwt(username, "access", "suyeon", 1000L * 60 * 60);
        assertEquals(username, jwtUtil.getUsername(token));
    }

    @Test
    public void getIsExpired_ShouldReturnIsExpired_WhenTokenPassed()
    {
        boolean isExpired = false;
        String token = jwtUtil.createJwt("testMember", "access", "suyeon", 1000L * 60 * 60);
        assertEquals(isExpired, jwtUtil.isRefreshable(token));
    }

    @Test
    public void getIsAccessToken_ShouldReturnIsAccessToken_WhenTokenPassed()
    {
        boolean isAccessToken = true;
        String token = jwtUtil.createJwt("testMember", "access", "suyeon", 1000L * 60 * 60);
        assertEquals(isAccessToken, jwtUtil.isAccessToken(token));
    }

    @Test
    public void getIsRefreshable_ShouldReturnIsRefreshable_WhenTokenPassed()
    {
        boolean isRefreshable = false;
        String token = jwtUtil.createJwt("testMember", "access", "suyeon", 1000L * 60 * 60);
        assertEquals(isRefreshable, jwtUtil.isRefreshable(token));
    }


    @Nested
    class ResolveTokenMethod {
        @Test
        public void shouldReturnToken_WhenValidToken() {
            when(request.getHeader("Authorization")).thenReturn("Bearer validToken");
            assertEquals("validToken", jwtUtil.resolveToken(request));
        }

        @Test
        public void shouldReturnNull_WhenInvalidToken() {
            when(request.getHeader("Authorization")).thenReturn("Invalid");
            assertNull(jwtUtil.resolveToken(request));
        }

        @Test
        public void shouldReturnNull_WhenNull() {
            when(request.getHeader("Authorization")).thenReturn(null);
            assertNull(jwtUtil.resolveToken(request));
        }
    }

}
