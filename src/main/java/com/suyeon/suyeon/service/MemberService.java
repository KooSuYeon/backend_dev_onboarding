package com.suyeon.suyeon.service;

import com.suyeon.suyeon.config.JwtUtil;
import com.suyeon.suyeon.dto.*;
import com.suyeon.suyeon.entity.Member;
import com.suyeon.suyeon.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final JwtUtil jwtUtil;
    private static final long ACCESS_TOKEN_VALIDITY_DURATION = 60 * 60 * 1000L;
    private static final long REFRESH_TOKEN_VALIDITY_DURATION = 90 * 24 * 60 * 60 * 1000L;

    public SignupResponseDto signup(SignupRequestDto requestDto)
    {
        if (memberRepository.existsByUsername(requestDto.getUsername()))
            throw new DuplicateKeyException("이미 사용 중인 ID 입니다!");

        Member member = new Member();
        member.setUsername(requestDto.getUsername());
        member.setNickname(requestDto.getNickname());
        member.setRole("ROLE_USER");
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
        member.setPassword(encodedPassword);
        memberRepository.save(member);

        List<AuthorityDto> authorities = new ArrayList<>();
        authorities.add(new AuthorityDto("ROLE_USER"));

        SignupResponseDto responseDto = modelMapper.map(member, SignupResponseDto.class);
        responseDto.setAuthorities(authorities);
        return responseDto;
    }

    public SignResponseDto sign(SignRequestDto requestDto, HttpServletResponse response)
    {
        Member member = memberRepository.findByUsername(requestDto.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저는 존재하지 않습니다 : " +requestDto.getUsername()));

        if (!passwordEncoder.matches(requestDto.getPassword(), member.getPassword())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다!");
        }
        String accessToken = jwtUtil.createJwt(member.getUsername(), "access", "suyeon", ACCESS_TOKEN_VALIDITY_DURATION);

        String refreshToken = member.getRefreshToken();
        if (refreshToken == null || jwtUtil.isExpired(refreshToken)) {
            refreshToken = jwtUtil.createJwt(member.getUsername(), "refresh", "suyeon", REFRESH_TOKEN_VALIDITY_DURATION);
            member.setRefreshToken(refreshToken);
            memberRepository.save(member);
        }


        Cookie refreshTokenCookie = new Cookie("refresh", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
        refreshTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshTokenCookie);

        SignResponseDto responseDto = new SignResponseDto();
        responseDto.setToken(accessToken);
        return responseDto;
    }

        public ProfileResponseDto profile(String username)
        {
            Member member = memberRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("해당 유저는 존재하지 않습니다 : " + username));

            return modelMapper.map(member, ProfileResponseDto.class);

        }

}
