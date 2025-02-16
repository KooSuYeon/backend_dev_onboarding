package com.suyeon.suyeon.controller;

import com.suyeon.suyeon.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;


@Tag(name = "Member API", description = "회원 서비스 API")
public interface SwaggerMemberController {

    @Operation(summary = "회원가입 API", description = "아이디,비밀번호,닉네임을 사용하여 새 멤버를 생성합니다.")
    @ApiResponse(
            responseCode = "201",
            description = "회원가입 성공 예시",
            content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SignupResponseDto.class))
            })
    ResponseEntity<SignupResponseDto> signup(
            @RequestBody SignupRequestDto dto);

    @Operation(summary = "로그인 API", description = "아이디과 비밀번호를 사용하여 새 멤버를 생성합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "로그인 성공 예시",
            content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SignResponseDto.class))
            })
    ResponseEntity<SignResponseDto> sign(
            @RequestBody SignRequestDto dto, HttpServletResponse response);

    @Operation(summary = "프로필 조회 API", description = "AccessToken을 이용해 인가하고 인가 정보로 프로필을 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "프로필 조회 성공 예시",
            content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProfileResponseDto.class))
            })
    ResponseEntity<ProfileResponseDto> profile(Authentication auth);
}
