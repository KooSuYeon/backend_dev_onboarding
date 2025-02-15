# backend_dev_onboarding_구수연

<a id="summary"></a>
### 📃 Spring Boot Project Setting 

- Version : 3.4.2
- Language : Java 17 

Dependencies
- Lombok: Getter/Setter, 생성자 등을 자동 생성하여 코드 간결화
- Spring Web: REST API 및 웹 애플리케이션 개발 지원
- Spring Boot DevTools: 코드 변경 시 자동 리스타트 및 개발 편의성 제공
- Spring Data JPA: ORM을 활용한 데이터베이스 접근 및 관리
- MySQL Driver: MySQL과의 연결을 위한 JDBC 드라이버

---
## 📑 목차
1. [📃 프로젝트 개요](#summary)
2. [📝 엔터티](#entity)
3. [🔐 Spring Security](#-springsecurity)
4. [🔑 JWT 세팅](#jwt-setting)
5. [🔄 토큰 발행과 검증](#access-refresh)
6. [🖋 API 명세서](#api-docs)
7. [🛠 JUnit 테스트](#junit)

---

<a id="entity"></a>
### 📝 엔터티

<details>
  <summary>클릭하여 엔티티 내용 보기</summary>

### **Member**
- `username` : 회원 고유 Username
- `password` : 회원 인증을 위한 비밀번호
- `nickname` : 회원 별명
- `createdAt`, `modifiedAt` : 회원 생성 및 수정 시기 (BaseTimeEntity 상속)

### **BaseTimeEntity**
- `createdAt` : 생성 시기
- `modifiedAt` : 수정 시기

</details>

---

### 🔐 SpringSecurity

<details>
  <summary>클릭하여 Security 설정 보기</summary>

### 1. SecurityFilterChain

```
@Bean
public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

```
SecurityFilterChain은 Spring Security의 필터 체인을 설정하는 객체입니다. Spring Security에서는 요청에 대한 인증과 권한 부여를 필터 체인을 통해 처리합니다. 

### 2. HTTP Basic 인증 비활성화

```
httpSecurity
    .httpBasic(AbstractHttpConfigurer::disable)
```
HTTP Basic 인증은 클라이언트가 username:password 형식의 인증 정보를 HTTP 헤더에 포함하여 서버에 전달하는 방식입니다. 해당 설정을 비활성화하고 header의 JWT토큰으로만 보안을 유지하고자 합니다.

### 3. CSRF 보호 비활성화

`.csrf(AbstractHttpConfigurer::disable)`

쿠키가 아닌 헤더의 Authorication에 JWT를 전송해 인증을 하기 때문에 해당 보안이 필요하지 않습니다.

### 4. JWT 필터 추가

```
.addFilterBefore(
        new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)

```

addFilterBefore() 메서드는 지정된 필터를 기존의 필터 체인에 추가하는데, UsernamePasswordAuthenticationFilter.class 이전에 JwtFilter를 추가하고 있습니다. 이렇게 하면 JWT 토큰이 먼저 검증되고, 이후 UsernamePasswordAuthenticationFilter가 인증 처리를 계속하도록 설정됩니다.

</details>

---

<a id="jwt-setting"></a>
### 🔑 JWT 세팅 

<details> <summary>클릭하여 JWT 설정 보기</summary>

Spring Security에서 Filter의 역할:

요청을 가로채서 처리: 필터는 서블릿 컨테이너 내에서 요청이 컨트롤러에 도달하기 전에 또는 응답이 클라이언트로 전송되기 전에 특정 로직을 실행할 수 있게 해줍니다. Spring Security의 JwtFilter와 같은 필터는 이 시점에서 JWT 토큰의 유효성을 검사하고 인증 작업을 수행합니다.

> JwtUtil

- JWT를 생성하고(createJwt)
- 토큰에서 username, type, role (관리자 역할이 필요한다면 추후 개발 예정) 정보 추출
- 만료 여부(isExpired), accessToken 갱신 여부(isRefreshable),  확인

> JwtFilter

- 매 요청마다 JWT를 확인하고
- 유효하면 SecurityContext에 인증 정보 저장
- 만료되었거나 잘못된 토큰이면 에러 메시지 반환

</details>

---
<a id="access-refresh"></a>
### 🔄 Access / Refresh Token 발행과 검증에 관한 시나리오 <a id="access--refresh-token-발행과-검증에-관한-시나리오"></a>

<details> <summary>클릭하여 토큰 발행 시나리오 보기</summary>

> 회원가입 (NO AUTH)

- 회원가입 시에 회원별로 주기가 30일인 Refresh Token이 발행되며 저장됩니다.

> 로그인 (NO AUTH)

- 회원가입된 유저가 로그인을 한다면
- Refresh Token의 생존 여부에 따라 만료가 되기 전이라면 1시간 주기의 Access Token을 발급해줍니다.
- 만료가 다 된 Refresh Token이라면 DB의 Refresh Token을 갱신해주고 (30일 더) 1시간 주기의 Access Token을 발급해줍니다.
- 로그인 응답 Response의 body로 AccessToken을 할당합니다.
- 로그인 응답 Cookie에 refreshToken을 할당해둡니다.
- 로그인 후에는 body에 나오는 AccessToken을 프론트 측에서 저장해두고 AUTH 페이지에서 꺼내 사용하여 사용하는 시나리오입니다.

> 프로필 조회 (AUTH)

- AccessToken에서 Filter를 거쳐 추출되는 username으로 DB를 조회해 프로필 정보를 보여줍니다.
- 만료가 된 AccessToken일 때 : 401 ERROR
- 만료되기 전 AccessToken일 때 : 200 OK
- 만료되기 전인데 갱신할 수 있는 AccessToken을 때 : 202 ACCEPTED
- 헤더에 AccessToken가 아니라 RefreshToken가 들어올 때 : 403 ERROR
- 헤더에 토큰이 들어오지 않을 때 : 401 ERROR

- 프론트 측에서는 1시간 주기의 로그인 (Auth) 가능 시간을 연장시키기 위해서는 
- 로그인 API (/api/members/sign)을 호출해 다시 body로 AccessToken을 받아와 헤더에 재할당 해줘 로그인을 연장해줍니다.
- 이때 보안을 위해서 프론트 측 Cookie에서 refreshToken를 꺼내 RefreshToken이 만료되었는지/Cookie에 RefreshToken이 있는지/RefreshToken의 username과 AccessToken의 username이 일치하는지
- 여부를 확인한 후에 모든 조건에 해당한다면은 갱신할 조건 (202 Accpeted)에 해당한다면 응답코드를 보내줍니다. 

---
### HTTPS 로 바꿔 보안 높이는 방법 (예정)

현재는 도메인 구입 전 (HTTP 환경)일 때에는
```
- refreshTokenCookie.setSecure(true); // HTTPS에서만 전송 (보안 강화)
- refreshTokenCookie.setAttribute("SameSite", "Strict"); // CSRF 공격 방지 강화
```

로 수정해줍니다. 

</details>

---
<a id="api-docs"></a>
### 🖋️ API 명세서 

<details> <summary>클릭하여 API 명세서 보기</summary>

1. 회원가입
- `/api/members/signup`
- Request (Body)
```
{
    "username" : "gu01416",
    "password" : "password1234!",
    "nickname" : "sooya"
}
```
- Response (Body)
```
{
    "username": "gu01416",
    "nickname": "sooya",
    "authorities": [
        {
            "authorityName": "ROLE_USER"
        }
    ]
}
```
---

2. 로그인 </br>
- `/api/members/sign`
- Request (Body)
```
{
    "username" : "gu01416",
    "password" : "password1234!"
}
```
- Response (Body)
```
{
    "token": "eyJhbGciOiJIUzI1NiJ9...."
}
```

- Response (Cookies)
```
{
    "Cookies": "refresh=ejkhsdjsdskdjsk..."
}
```
---
3. 회원조회 </br>
- `/api/members/profile`
- Request (Header)
```
{
    "Authorization": "Bearer eyJhbGciOiJIUzI1NiJ9...",
    "Cookies": "refresh=ejkhsdjsdskdjsk..."
}
```
- Response (Body)
```
{
    "username": "gu01416",
    "nickname": "sooya"
}
```
</details>

---
<a id="junit"></a>
### 🛠️ JUnit를 이용한 테스트 코드 작성 <a id="junit를-이용한-테스트-코드-작성"></a>

<details> <summary>클릭하여 테스트 코드 보기</summary>

- 테스트 코드 메서드 이름 규칙: 응답코드_테스트 상황

> JwtUtilTest: JWT Token 관련 테스트 코드

- 토큰 생성/토큰 만료/토큰 Username 추출/토큰 타입 AccessToken인지/토큰 Refreshable할 타이밍인지/헤더에서 토큰 추출합니다.
- JwtUtil : signed된 Jwt인지 확인하는 코드 추가
- JwtFilter : ErrorResponse 응답 부분 세분화 및 refreshToken 코드 최적화
- 202 : 프론트 측에서 헤더 변경 요청일 때 (토큰은 유효할 때)
- 403 : Access Token을 사용해야 하는데 Refresh Token을 사용했을 때
- 401 : 헤더에 토큰이 없을 때
- 401 : AccessToken Refresh 시도 할 때 쿠키에 RefreshToken 존재하지 않을 때

> MemberControllerTest: 회원관련 ControllerTest

- 클래스별로 회원가입/로그인/프로필 조회 테스트 작성합니다.

No Auth (Spring Security를 적용할 필요가 없기 때문에 빠른 테스트, 단위테스트를 적용합니다. (MockitoExtension.class))
- 회원가입 : 정상 생성 (201), 이미 존재하는 username인데 생성 (409)
- 로그인 : 정상 로그인 (200), 회원 존재 안할 때 로그인 시도 (404), 비밀번호 오류 (401)
</br>

Auth (통합 테스트를 위한 SpringExtension.class 적용합니다.)
- 프로필 조회 : 정상 조회 (200), (Auth 자체가 JWT 토큰으로 하고 있기 때문에 에러 부분은 JwtUtilTest에서 동작합니다.)
- WithMockCustomUser : 테스트 코드 동작을 위한 사용자 생성
- WithCustomMockUserSecurityContextFactory: 사용자가 통과할 SecurityContext 생성

</details>
