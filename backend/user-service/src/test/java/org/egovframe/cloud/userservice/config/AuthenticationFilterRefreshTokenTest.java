package org.egovframe.cloud.userservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.egovframe.cloud.userservice.api.user.dto.UserResponseDto;
import org.egovframe.cloud.userservice.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * refresh token 으로 들어온 요청을 AuthenticationFilter 가 처리하는 경로의 회귀 테스트.
 *
 * refresh token 에는 authorities 클레임이 없다(TokenProvider.createRefreshToken).
 * 같은 저장소의 나머지 AuthenticationFilter 세 곳(module-common, board-service,
 * portal-service)은 authorities 가 없을 수 있다고 보고 null 을 빈 목록으로 처리한다.
 */
class AuthenticationFilterRefreshTokenTest {

    private static final String SECRET = "egovframe_user_token";
    private static final String USER_ID = "USER_0000000000000000001";
    private static final String EMAIL = "test@egovframe.org";

    private UserService userService;
    private TokenProvider tokenProvider;
    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        tokenProvider = new TokenProvider(userService);
        ReflectionTestUtils.setField(tokenProvider, "TOKEN_SECRET", SECRET);
        ReflectionTestUtils.setField(tokenProvider, "TOKEN_EXPIRATION_TIME", "3600000");
        ReflectionTestUtils.setField(tokenProvider, "TOKEN_REFRESH_TIME", "86400000");
        filter = new AuthenticationFilter(mock(AuthenticationManager.class), tokenProvider, userService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 로그인 성공 시 애플리케이션이 실제로 발급하는 refresh token 을 그대로 얻는다.
     */
    private String issuedRefreshToken() {
        UserResponseDto userResponseDto = mock(UserResponseDto.class);
        when(userResponseDto.getUserId()).thenReturn(USER_ID);
        when(userService.findByEmail(anyString())).thenReturn(userResponseDto);

        Authentication authResult = new UsernamePasswordAuthenticationToken(
                EMAIL, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        MockHttpServletResponse response = new MockHttpServletResponse();
        tokenProvider.createTokenAndAddHeader(
                new MockHttpServletRequest(), response, new MockFilterChain(), authResult);

        return response.getHeader("refresh-token");
    }

    @Test
    @DisplayName("발급된 refresh token 에는 authorities 클레임이 없다")
    void refreshTokenHasNoAuthoritiesClaim() {
        String refreshToken = issuedRefreshToken();

        assertThat(refreshToken).isNotNull();
        assertThat(tokenProvider.getClaimsFromToken(refreshToken).getSubject()).isEqualTo(USER_ID);
        assertThat(tokenProvider.getClaimsFromToken(refreshToken)
                .get(tokenProvider.TOKEN_CLAIM_NAME, String.class)).isNull();
    }

    @Test
    @DisplayName("refresh token 재발급 요청이 필터를 통과한다")
    void refreshTokenRequestPassesThroughFilter() {
        String refreshToken = issuedRefreshToken();

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/users/token/refresh");
        request.addHeader(HttpHeaders.AUTHORIZATION, refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertThatCode(() -> filter.doFilter(request, response, chain)).doesNotThrowAnyException();
        assertThat(chain.getRequest())
                .as("필터가 재발급 요청을 컨트롤러로 넘겨야 한다")
                .isNotNull();
    }

    @Test
    @DisplayName("권한이 없는 refresh token 은 빈 권한으로 처리한다")
    void refreshTokenGrantsNoAuthorities() {
        String refreshToken = issuedRefreshToken();

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/users/token/refresh");
        request.addHeader(HttpHeaders.AUTHORIZATION, refreshToken);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }
}
