package org.egovframe.cloud.reserveitemservice.domain.reserveItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.egovframe.cloud.reserveitemservice.api.reserveItem.dto.ReserveItemRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * org.egovframe.cloud.reserveitemservice.domain.reserveItem.ReserveItemRepositoryImplSearchParamTest
 * <p>
 * 예약 물품 목록 검색조건 조립 단위 테스트
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
class ReserveItemRepositoryImplSearchParamTest {

    private final ReserveItemRepositoryImpl repository = new ReserveItemRepositoryImpl(null);

    @SuppressWarnings("unchecked")
    private List<Criteria> whereQuery(ReserveItemRequestDto requestDto) {
        return (List<Criteria>) ReflectionTestUtils.invokeMethod(repository, "whereQuery", requestDto);
    }

    @DisplayName("isUse 를 보내지 않아도 검색조건을 만들 수 있다")
    @Test
    void whereQuery_without_isUse() {
        ReserveItemRequestDto requestDto = new ReserveItemRequestDto();

        assertThatCode(() -> whereQuery(requestDto)).doesNotThrowAnyException();
        assertThat(whereQuery(requestDto)).isEmpty();
    }

    @DisplayName("isUse 가 true 이면 사용여부 조건이 붙는다")
    @Test
    void whereQuery_with_isUse_true() {
        ReserveItemRequestDto requestDto = new ReserveItemRequestDto();
        requestDto.setIsUse(true);

        assertThat(whereQuery(requestDto))
                .singleElement()
                .satisfies(criteria -> assertThat(criteria.toString()).contains("use_at"));
    }

    @DisplayName("isUse 가 false 이면 사용여부 조건이 붙지 않는다")
    @Test
    void whereQuery_with_isUse_false() {
        ReserveItemRequestDto requestDto = new ReserveItemRequestDto();
        requestDto.setIsUse(false);

        assertThat(whereQuery(requestDto)).isEmpty();
    }

    @DisplayName("isUse 와 isPopup 이 함께 오면 두 조건이 붙는다")
    @Test
    void whereQuery_with_isUse_and_isPopup() {
        ReserveItemRequestDto requestDto = new ReserveItemRequestDto();
        requestDto.setIsUse(true);
        requestDto.setIsPopup(true);

        assertThat(whereQuery(requestDto))
                .hasSize(2)
                .anySatisfy(criteria -> assertThat(criteria.toString()).contains("use_at"))
                .anySatisfy(criteria -> assertThat(criteria.toString()).contains("reserve_method_id"));
    }
}
