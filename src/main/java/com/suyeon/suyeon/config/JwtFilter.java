package com.suyeon.suyeon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suyeon.suyeon.exception.TokenUpgradeRequiredException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter{

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String servletPath = request.getServletPath();

        if (isExemptPath(servletPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String token = jwtUtil.resolveToken(request);

            if (!jwtUtil.isAccessToken(token))
            {
                sendErrorResponse(response, "ACCESS Token만 보안 토큰으로 이용가능힙니다!");
                return;
            }

            if (jwtUtil.isRefreshable(token) && !jwtUtil.isExpired(token)) {

                Cookie refreshTokenCookie = getRefreshTokenCookie(request);
                String refreshToken = refreshTokenCookie.getValue();

                if (jwtUtil.isExpired(refreshToken)) {
                    sendErrorResponse(response, "Refresh Token이 만료되었습니다! 다시 로그인하세요.");
                    return;
                }

                String username = jwtUtil.getUsername(token);
                long ACCESS_TOKEN_VALIDITY_DURATION = 60 * 60 * 1000L;
                String accessToken = jwtUtil.createJwt(username, "access", "suyeon", ACCESS_TOKEN_VALIDITY_DURATION);

                throw new TokenUpgradeRequiredException("프론트 측에서 Authorization의 AccessToken 갱신 요청이 필요합니다!", accessToken);
            }

            if (!jwtUtil.isExpired(token)) {
                String username = jwtUtil.getUsername(token);

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }

            sendErrorResponse(response, "만료된 토큰입니다! 다시 로그인하세요!");

        } catch (NullPointerException | ServletException e) {
            sendErrorResponse(response, "인증이 필요한 토큰입니다!");
        } catch (MalformedJwtException e) {
            sendErrorResponse(response, "손상된 토큰입니다! 다시 로그인하세요!");
        } catch (ExpiredJwtException e) {
            sendErrorResponse(response, "만료된 토큰입니다! 다시 로그인하세요!");
        } catch (UnsupportedJwtException e) {
            sendErrorResponse(response, "지원하지 않은 토큰입니다!");
        } catch (IllegalArgumentException e) {
            sendErrorResponse(response, "클레임이 비어있는 토큰입니다!");
        } catch (TokenUpgradeRequiredException e)
        {
            sendUpgradeResponse(response, e);
        }
    }

    private Cookie getRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refresh".equals(cookie.getName()))
                .findFirst()
                .orElse(null);
    }

    private boolean isExemptPath(String servletPath) {
        return servletPath.startsWith("/api/members/signup")
                || servletPath.equals("/api/members/sign")
                || servletPath.equals("/health")
                || servletPath.startsWith("/favicon.ico");
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(FORBIDDEN.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().write(message);
    }

    private void sendUpgradeResponse(HttpServletResponse response, TokenUpgradeRequiredException e) throws IOException {
        response.setStatus(HttpStatus.UPGRADE_REQUIRED.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", e.getMessage());
        responseBody.put("access", e.getAccessToken());

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}
