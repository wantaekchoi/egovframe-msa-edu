package org.egovframe.cloud.userservice.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.egovframe.cloud.common.util.MessageUtil;
import org.egovframe.cloud.userservice.domain.log.LoginLog;
import org.egovframe.cloud.userservice.domain.log.LoginLogRepository;
import org.egovframe.cloud.userservice.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * org.egovframe.cloud.userservice.service.user.UserServiceLoginCallbackTest
 * <p>
 * 로그인 후처리에서 가입되지 않은 이메일을 다루는 회귀 테스트.
 *
 * AuthenticationFilter.unsuccessfulAuthentication 은 "해당 사용자가 없습니다" 를 별도
 * 분기로 다루면서 loginCallback 에 실패 사유를 넘긴다. 로그인 실패 감사 로그는 가입
 * 여부와 무관하게 남아야 한다.
 *
 * @author 표준프레임워크센터
 * @version 1.0
 * @since 2026/08/28
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2026/08/28    contributors  최초 생성
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class UserServiceLoginCallbackTest {

    private static final Long SITE_ID = 1L;
    private static final String UNKNOWN_EMAIL = "notjoined@egovframe.org";

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginLogRepository loginLogRepository;

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<LoginLog> loginLogCaptor;

    @BeforeEach
    void bindRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        ReflectionTestUtils.setField(userService, "messageUtil", messageUtil);
    }

    @AfterEach
    void unbindRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @DisplayName("가입되지 않은 이메일의 로그인 실패도 후처리를 마친다")
    @Test
    void loginCallback_with_unknown_email() {
        given(userRepository.findByEmail(UNKNOWN_EMAIL)).willReturn(Optional.empty());

        assertThatCode(() -> userService.loginCallback(SITE_ID, UNKNOWN_EMAIL, false, "해당 사용자가 없습니다"))
                .doesNotThrowAnyException();
    }

    @DisplayName("가입되지 않은 이메일의 로그인 실패도 로그인 로그로 남는다")
    @Test
    void loginCallback_saves_login_log_for_unknown_email() {
        given(userRepository.findByEmail(UNKNOWN_EMAIL)).willReturn(Optional.empty());

        userService.loginCallback(SITE_ID, UNKNOWN_EMAIL, false, "해당 사용자가 없습니다");

        verify(loginLogRepository).save(loginLogCaptor.capture());
        assertThat(loginLogCaptor.getValue().getEmail()).isEqualTo(UNKNOWN_EMAIL);
        assertThat(loginLogCaptor.getValue().getSuccessAt()).isFalse();
    }
}
