package com.suyeon.suyeon.controller;


import com.suyeon.suyeon.dto.*;
import com.suyeon.suyeon.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController implements SwaggerMemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(
            @RequestBody SignupRequestDto dto) {
        SignupResponseDto responseDto = memberService.signup(dto);

        return ResponseEntity.status(CREATED).body(responseDto);
    }

    @PostMapping("/sign")
    public ResponseEntity<SignResponseDto> sign(
            @RequestBody SignRequestDto dto, HttpServletResponse response) {
        SignResponseDto responseDto = memberService.sign(dto, response);

        return ResponseEntity.status(OK).body(responseDto);
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponseDto> profile(Authentication auth) {

        ProfileResponseDto responseDto = memberService.profile(auth.getName());

        return ResponseEntity.status(OK).body(responseDto);
    }
}
