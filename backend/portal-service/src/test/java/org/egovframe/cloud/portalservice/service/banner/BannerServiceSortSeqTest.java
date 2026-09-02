package org.egovframe.cloud.portalservice.service.banner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.egovframe.cloud.portalservice.api.banner.dto.BannerUpdateRequestDto;
import org.egovframe.cloud.portalservice.domain.banner.Banner;
import org.egovframe.cloud.portalservice.domain.banner.BannerRepository;
import org.egovframe.cloud.portalservice.domain.menu.Site;
import org.egovframe.cloud.portalservice.domain.menu.SiteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * org.egovframe.cloud.portalservice.service.banner.BannerServiceSortSeqTest
 * <p>
 * 배너 수정 시 정렬 순서 조정 단위 테스트
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
class BannerServiceSortSeqTest {

    private static final Integer BANNER_NO = 1;
    private static final Long SITE_ID = 1L;

    @Mock
    private BannerRepository bannerRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private BannerService bannerService;

    private void givenBannerAt(Integer sortSeq) {
        Banner entity = Banner.builder()
                .bannerNo(BANNER_NO)
                .bannerTypeCode("main")
                .bannerTitle("배너")
                .sortSeq(sortSeq)
                .build();
        given(bannerRepository.findById(BANNER_NO)).willReturn(Optional.of(entity));
        given(siteRepository.findById(SITE_ID)).willReturn(Optional.of(Site.builder().build()));
        // 조회는 수정 대상 자신도 걸러내지 않는다.
        given(bannerRepository.findBySortSeqAndSiteId(any(), any())).willReturn(Optional.empty());
        given(bannerRepository.findBySortSeqAndSiteId(sortSeq, SITE_ID)).willReturn(Optional.of(entity));
    }

    private BannerUpdateRequestDto requestWithSortSeq(Integer sortSeq) {
        BannerUpdateRequestDto requestDto = new BannerUpdateRequestDto();
        ReflectionTestUtils.setField(requestDto, "bannerTypeCode", "main");
        ReflectionTestUtils.setField(requestDto, "bannerTitle", "배너");
        ReflectionTestUtils.setField(requestDto, "sortSeq", sortSeq);
        ReflectionTestUtils.setField(requestDto, "siteId", SITE_ID);
        return requestDto;
    }

    @DisplayName("배너 수정 시 정렬 순서가 밀려나면 사이 구간 정렬 순서를 1씩 줄인다")
    @Test
    void should_decreaseSortSeqBetweenRange_when_sortSeqIsPostponed() {
        givenBannerAt(1);

        bannerService.update(BANNER_NO, requestWithSortSeq(3));

        verify(bannerRepository).updateSortSeq(2, 3, -1, SITE_ID);
    }

    @DisplayName("배너 수정 시 정렬 순서가 당겨지면 사이 구간 정렬 순서를 1씩 늘린다")
    @Test
    void should_increaseSortSeqBetweenRange_when_sortSeqIsAdvanced() {
        givenBannerAt(3);

        bannerService.update(BANNER_NO, requestWithSortSeq(1));

        verify(bannerRepository).updateSortSeq(1, 2, 1, SITE_ID);
    }

    @DisplayName("배너 수정 시 정렬 순서가 그대로면 다른 배너를 건드리지 않는다")
    @Test
    void should_notTouchOtherBanners_when_sortSeqIsUnchanged() {
        givenBannerAt(2);

        bannerService.update(BANNER_NO, requestWithSortSeq(2));

        verify(bannerRepository, never()).updateSortSeq(anyInt(), any(), anyInt(), anyLong());
    }
}
