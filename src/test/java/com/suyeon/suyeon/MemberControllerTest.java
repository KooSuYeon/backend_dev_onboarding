package com.suyeon.suyeon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suyeon.suyeon.config.JwtUtil;
import com.suyeon.suyeon.controller.MemberController;
import com.suyeon.suyeon.dto.*;
import com.suyeon.suyeon.entity.Member;
import com.suyeon.suyeon.exception.GlobalExceptionHandler;
import com.suyeon.suyeon.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class MemberControllerTest {

    private MockMvc mockMvc;
    @Mock
    private MemberService memberService;
    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;
    private static final long ACCESS_TOKEN_VALIDITY_DURATION = 60 * 60 * 1000L;
    private static final long REFRESH_TOKEN_VALIDITY_DURATION = 90 * 24 * 60 * 1000L;

    @InjectMocks
    private MemberController memberController;

    @BeforeEach
    public void setup() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(memberController)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Nested
    class signupMethod {

        @Test
        public void shouldReturn201_WhenUsernameAndPasswordIsPassed() throws Exception {
            SignupRequestDto requestDto =
                    new SignupRequestDto("testMember", "password", "nickname");
            String reqBody = new ObjectMapper().writeValueAsString(requestDto);

            Member member = new Member();
            member.setId(1L);
            member.setUsername(requestDto.getUsername());
            member.setRole("ROLE_USER");
            String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
            member.setPassword(encodedPassword);

            List<AuthorityDto> authorities = new ArrayList<>();
            authorities.add(new AuthorityDto("ROLE_USER"));


            given(memberService.signup(any(SignupRequestDto.class)))
                    .willReturn(new SignupResponseDto(member.getUsername(), member.getNickname(), authorities));

            mockMvc
                    .perform(
                            post("/api/members/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(reqBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("testMember"));

        }

        @Test
        public void shouldReturn409_WhenUsernameExists() throws Exception {

            SignupRequestDto requestDto =
                    new SignupRequestDto("testMember", "password", "nickname");
            String reqBody = new ObjectMapper().writeValueAsString(requestDto);

            when(memberService.signup(any(SignupRequestDto.class)))
                    .thenThrow(new DuplicateKeyException("이미 사용 중인 ID 입니다!"));

            mockMvc
                    .perform(
                            post("/api/members/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(reqBody))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class signMethod {

        @Test
        public void shouldReturn200_WhenLoginIsPassed() throws Exception
        {
            SignRequestDto requestDto = new SignRequestDto("testMember", "password");
            String reqBody = new ObjectMapper().writeValueAsString(requestDto);

            String accessToken = jwtUtil.createJwt("testMember", "access", "suyeon", ACCESS_TOKEN_VALIDITY_DURATION);
            SignResponseDto responseDto =
                    new SignResponseDto(accessToken);

            given(memberService.sign(any(SignRequestDto.class), any(HttpServletResponse.class))).willReturn(responseDto);


            mockMvc
                    .perform(
                            post("/api/members/sign").contentType(MediaType.APPLICATION_JSON).content(reqBody))
                                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(accessToken));


        }

        @Test
        public void shouldReturn401_WhenPasswordIsWrong() throws Exception
        {
            SignRequestDto requestDto = new SignRequestDto("testMember", "password");
            String reqBody = new ObjectMapper().writeValueAsString(requestDto);

            given(memberService.sign(any(SignRequestDto.class), any(HttpServletResponse.class)))
                    .willThrow(new BadCredentialsException("잘못된 비밀번호입니다."));

            mockMvc
                    .perform(
                            post("/api/members/sign").contentType(MediaType.APPLICATION_JSON).content(reqBody))
                    .andExpect(status().isUnauthorized());

        }

        @Test
        public void shouldReturn404_WhenMemberIsNotExisted() throws Exception
        {
            SignRequestDto requestDto = new SignRequestDto("testMember", "password");
            String reqBody = new ObjectMapper().writeValueAsString(requestDto);

            given(memberService.sign(any(SignRequestDto.class), any(HttpServletResponse.class)))
                    .willThrow(new UsernameNotFoundException("존재하지 않는 회원입니다."));

            mockMvc
                    .perform(
                            post("/api/members/sign").contentType(MediaType.APPLICATION_JSON).content(reqBody))
                    .andExpect(status().isNotFound());

        }
    }

    @Nested
    class profileMethod {

        @Test
        @ExtendWith(SpringExtension.class)
        @WithMockCustomUser
        public void shouldReturn200_WhenProfileIsRequested() throws Exception {
            String username = "testMember";
            String nickname = "nickname";
            ProfileResponseDto responseDto = new ProfileResponseDto(username, nickname);

            Authentication authentication = SecurityContextHolder.getContext()
                    .getAuthentication();
            System.out.println(authentication);


            when(memberService.profile(username)).thenReturn(responseDto);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            mockMvc.perform(get("/api/members/profile")
                                    .principal(new UsernamePasswordAuthenticationToken(username, null)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.nickname").value(nickname));
        }
    }
}
