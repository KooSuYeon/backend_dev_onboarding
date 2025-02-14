package com.suyeon.suyeon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
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

            if (!jwtUtil.isAccessToken(token)) {
                String refreshToken = getRefreshTokenCookie(request);
                if (refreshToken == null) {
                    sendErrorResponse(response, "Refresh Token이 없습니다! 재로그인하세요!");
                    return;
                }

                log.info("REFRESH : {}", refreshToken);

                if (jwtUtil.isExpired(refreshToken)) {
                    sendErrorResponse(response, "Refresh Token이 만료되었습니다! 재로그인하세요!");
                    return;
                }

                sendErrorResponse(response, "Access Token을 사용했는지 확인하시거나 만료되었는지를 확인하세요!");
                return;
            }

            if (jwtUtil.isRefreshable(token) && !jwtUtil.isExpired(token)) {
                String refreshToken = getRefreshTokenCookie(request);
                if (refreshToken == null || jwtUtil.isExpired(refreshToken)) {
                    sendErrorResponse(response, "Refresh Token이 만료되었습니다! 재로그인하세요!");
                    return;
                }

                String username = jwtUtil.getUsername(token);
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(response) {
                    @Override
                    public void setStatus(int sc) {
                        super.setStatus(HttpStatus.ACCEPTED.value());
                    }
                };

                filterChain.doFilter(request, responseWrapper);
                return;
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

        }  catch (MalformedJwtException e) {
            sendErrorResponse(response, "손상된 토큰입니다! 다시 로그인하세요!");
        } catch (ExpiredJwtException e) {
            sendErrorResponse(response, "만료된 토큰입니다! 다시 로그인하세요!");
        } catch (UnsupportedJwtException e) {
            sendErrorResponse(response, "지원하지 않은 토큰입니다!");
        } catch (IllegalArgumentException e) {
            sendErrorResponse(response, "클레임이 비어있는 토큰입니다!");
        } catch (Exception e) {
            sendErrorResponse(response, "알 수 없는 오류가 발생했습니다!");
        }
    }

    private String getRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        Cookie refreshCookie = null;

        for (Cookie cookie : cookies) {
            if ("refresh".equals(cookie.getName())) {
                refreshCookie = cookie;
                break;
            }
        }
        if (refreshCookie == null) {
            return null;
        } else {
            return refreshCookie.getValue();
        }
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
        response.setContentType("application/json");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", message);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}
