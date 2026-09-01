package org.egovframe.cloud.userservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.egovframe.cloud.servlet.exception.ExceptionHandlerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(profiles = "test")
class ExceptionHandlerAdviceRegistrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void 공통모듈_예외_어드바이스가_빈으로_등록된다() {
        System.out.println("ExceptionHandlerAdvice beans = "
                + applicationContext.getBeansOfType(ExceptionHandlerAdvice.class).keySet());

        assertThat(applicationContext.getBeansOfType(ExceptionHandlerAdvice.class)).isNotEmpty();
    }
}
