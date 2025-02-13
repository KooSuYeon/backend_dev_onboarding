package com.suyeon.suyeon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequestDto {

    private String username;
    private String password;
    private String nickname;
}
