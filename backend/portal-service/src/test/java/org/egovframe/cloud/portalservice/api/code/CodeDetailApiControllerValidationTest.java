package org.egovframe.cloud.portalservice.api.code;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egovframe.cloud.portalservice.domain.code.CodeRepository;
import org.egovframe.cloud.portalservice.service.code.CodeDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * org.egovframe.cloud.portalservice.api.code.CodeDetailApiControllerValidationTest
 * <p>
 * 공통코드 상세 수정 요청 검증 단위 테스트
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
class CodeDetailApiControllerValidationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CodeDetailApiController(mock(CodeDetailService.class), mock(CodeRepository.class)))
                .build();
    }

    @DisplayName("상위 코드ID 가 비어 있으면 수정 요청이 거부된다")
    @Test
    void update_rejects_blank_parent_code_id() throws Exception {
        mockMvc.perform(put("/api/v1/code-details/{codeId}", "TEST_DETAIL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentCodeId\":\"\",\"codeName\":\"테스트 코드\"}"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("코드 명이 비어 있으면 수정 요청이 거부된다")
    @Test
    void update_rejects_blank_code_name() throws Exception {
        mockMvc.perform(put("/api/v1/code-details/{codeId}", "TEST_DETAIL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentCodeId\":\"TEST_CODE\",\"codeName\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("필수 값이 모두 있으면 수정 요청이 통과한다")
    @Test
    void update_accepts_required_values() throws Exception {
        mockMvc.perform(put("/api/v1/code-details/{codeId}", "TEST_DETAIL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentCodeId\":\"TEST_CODE\",\"codeName\":\"테스트 코드\"}"))
                .andExpect(status().isOk());
    }
}
