package org.egovframe.cloud.reserverequestservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Collections;

import org.egovframe.cloud.common.dto.AttachmentEntityMessage;
import org.egovframe.cloud.reserverequestservice.api.dto.ReserveSaveRequestDto;
import org.egovframe.cloud.reserverequestservice.domain.Reserve;
import org.egovframe.cloud.reserverequestservice.domain.ReserveRepository;
import org.egovframe.cloud.reserverequestservice.domain.ReserveValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import reactor.core.publisher.Mono;

/**
 * org.egovframe.cloud.reserverequestservice.service.ReserveServiceAttachmentEventTest
 * <p>
 * 예약 신청 두 경로가 첨부파일 바인딩 이벤트를 발행하는지 확인하는 단위 테스트
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
@MockitoSettings(strictness = Strictness.LENIENT)
class ReserveServiceAttachmentEventTest {

    private static final String USER_ID = "user-1";
    private static final String ATTACHMENT_CODE = "ATT-1";

    @Mock
    private ReserveRepository reserveRepository;

    @Mock
    private ReserveValidator reserveValidator;

    @Mock
    private StreamBridge streamBridge;

    @Mock
    private AmqpAdmin amqpAdmin;

    @InjectMocks
    private ReserveService reserveService;

    @Captor
    private ArgumentCaptor<Message<AttachmentEntityMessage>> messageCaptor;

    private ReserveSaveRequestDto saveRequestDto() {
        return ReserveSaveRequestDto.builder()
                .reserveItemId(101L)
                .locationId(7L)
                .categoryId("space")
                .reserveQty(1)
                .reservePurposeContent("회의실 예약 사유")
                .attachmentCode(ATTACHMENT_CODE)
                .reserveStartDate(LocalDateTime.of(2026, 6, 1, 10, 0))
                .reserveEndDate(LocalDateTime.of(2026, 6, 1, 12, 0))
                .userId(USER_ID)
                .userContactNo("010-0000-0000")
                .userEmail("user@example.com")
                .build();
    }

    private void givenSavedReserve() {
        given(reserveValidator.checkSpace(any())).willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        given(reserveValidator.checkEquipment(any())).willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        given(reserveRepository.insert(any(Reserve.class)))
                .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    private <T> T withLogin(Mono<T> mono) {
        return mono.contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, Collections.emptyList()))).block();
    }

    @DisplayName("실시간 예약 신청도 첨부파일 바인딩 이벤트를 발행한다")
    @Test
    void save_publishes_attachment_event() {
        givenSavedReserve();

        withLogin(reserveService.save(saveRequestDto()));

        verify(streamBridge).send(anyString(), messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload().getAttachmentCode()).isEqualTo(ATTACHMENT_CODE);
        assertThat(messageCaptor.getValue().getPayload().getEntityId()).isNotNull();
    }

    @DisplayName("심사 예약 신청은 첨부파일 바인딩 이벤트를 발행한다")
    @Test
    void create_publishes_attachment_event() {
        givenSavedReserve();

        withLogin(reserveService.create(saveRequestDto()));

        verify(streamBridge).send(anyString(), messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload().getAttachmentCode()).isEqualTo(ATTACHMENT_CODE);
    }
}
