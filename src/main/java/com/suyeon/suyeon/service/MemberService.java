package com.suyeon.suyeon.service;

import com.suyeon.suyeon.dto.AuthorityDto;
import com.suyeon.suyeon.dto.SignupRequestDto;
import com.suyeon.suyeon.dto.SignupResponseDto;
import com.suyeon.suyeon.entity.Member;
import com.suyeon.suyeon.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

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
}
