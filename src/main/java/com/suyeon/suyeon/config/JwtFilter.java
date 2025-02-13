package com.suyeon.suyeon.config;

import com.suyeon.suyeon.repository.MemberRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.springframework.http.HttpStatus.FORBIDDEN;

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

            // 만료 30분 전일때 유효한 동작이 존재한다면 자동으로 refresh Token 받아오도록 추가 (예정)


            // 만료 전이라면 유효한 회원 찾을 수 있도록 (완료)
            if (!jwtUtil.isExpired(token)) {
                Long id = jwtUtil.getId(token);
                UserDetails userDetails = new UserDetails() {
                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        Collection<GrantedAuthority> collection = new ArrayList<>();
                        collection.add(new GrantedAuthority() {
                            @Override
                            public String getAuthority() {
                                return jwtUtil.getRole(token);
                            }
                        });
                        return collection;
                    }

                    @Override
                    public String getPassword() {
                        return null;
                    }

                    @Override
                    public String getUsername() {
                        return null;
                    }

                    public Long getId()
                    {
                        return id;
                    }

                };

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }

            sendErrorResponse(response, "만료된 토큰입니다! 다시 로그인하세요!");

        } catch (AuthenticationException | NullPointerException | ServletException e) {
            sendErrorResponse(response, "인증이 필요한 토큰입니다!");
        } catch (MalformedJwtException e) {
            sendErrorResponse(response, "손상된 토큰입니다! 다시 로그인하세요!");
        } catch (ExpiredJwtException e) {
            sendErrorResponse(response, "만료된 토큰입니다! 다시 로그인하세요!");
        } catch (UnsupportedJwtException e) {
            sendErrorResponse(response, "지원하지 않은 토큰입니다!");
        } catch (IllegalArgumentException e) {
            sendErrorResponse(response, "클레임이 비어있는 토큰입니다!");
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
        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().write(message);
    }
}
