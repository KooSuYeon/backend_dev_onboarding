package com.suyeon.suyeon;

import com.suyeon.suyeon.entity.Member;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.ArrayList;
import java.util.List;

public class WithMockCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        Member member = new Member();
        member.setId(customUser.id());
        member.setUsername(customUser.username());
        member.setPassword(customUser.password());
        member.setNickname(customUser.nickname());
        member.setRole(customUser.role());

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(customUser.role()));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                member, null, authorities);
        context.setAuthentication(auth);
        return context;
    }
}
