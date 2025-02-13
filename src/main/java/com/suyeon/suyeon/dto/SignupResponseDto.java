package com.suyeon.suyeon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SignupResponseDto {

    private String username;
    private String nickname;
    private List<AuthorityDto> authorities;
}
