package org.egovframe.cloud.portalservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.egovframe.cloud.servlet.exception.ExceptionHandlerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "test")
class ExceptionHandlerAdviceRegistrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void 공통모듈_예외_어드바이스가_빈으로_등록된다() {
        System.out.println("ExceptionHandlerAdvice beans = "
                + applicationContext.getBeansOfType(ExceptionHandlerAdvice.class).keySet());

        assertThat(applicationContext.getBeansOfType(ExceptionHandlerAdvice.class)).isNotEmpty();
    }

    @Test
    void 존재하지_않는_이미지_요청은_JSON_오류응답을_반환한다() {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity("/api/v1/images/no-such-unique-id", String.class);

        System.out.println("status = " + responseEntity.getStatusCode()
                + ", contentType = " + responseEntity.getHeaders().getContentType()
                + ", body = " + responseEntity.getBody());

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(responseEntity.getBody()).contains("\"code\":\"E003\"");
    }
}
