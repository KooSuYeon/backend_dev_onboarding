package com.suyeon.suyeon.controller;


import com.suyeon.suyeon.dto.SignupRequestDto;
import com.suyeon.suyeon.dto.SignupResponseDto;
import com.suyeon.suyeon.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(
            @RequestBody SignupRequestDto dto) {
        SignupResponseDto responseDto = memberService.signup(dto);

        return ResponseEntity.status(CREATED).body(responseDto);
    }
}
