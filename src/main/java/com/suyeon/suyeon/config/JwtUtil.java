package com.suyeon.suyeon.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import io.jsonwebtoken.Jwts;

@Component
public class JwtUtil {

    private SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {

        secretKey =
                new SecretKeySpec(
                        secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String createJwt(String username, String type, String issuer, Long expiredMs) {

        return Jwts.builder()
                .claim("username", username)
                .claim("type", type)
                .claim("iss", issuer)

                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }


    public String getUsername(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("username", String.class);
    }

    public Boolean isExpired(String token) {

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean isAccessToken(String token) {
        try {
            String tokenType = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("type", String.class);

            return "access".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }



    public Boolean isRefreshable(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();

            if (issuedAt == null || expiration == null) {
                return false;
            }

            long halfLife = (expiration.getTime() - issuedAt.getTime()) / 2;
            long refreshThreshold = issuedAt.getTime() + halfLife;

            return new Date().getTime() >= refreshThreshold;
        } catch (Exception e) {
            return false;
        }
    }




    public String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.split(" ")[1];
        }
        return null;
    }


}
