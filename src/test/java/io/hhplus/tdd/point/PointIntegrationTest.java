package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 통합 테스트: Controller → Service → Domain → Repository 전체 레이어 검증
 *
 * 테스트 격리 전략:
 * - 각 테스트는 서로 다른 userId를 사용하여 데이터 충돌 방지
 * - In-memory 저장소 특성상 @Transactional 대신 userId 분리 전략 사용
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("포인트 시스템 통합 테스트")
class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("포인트 충전 후 조회 - 전체 플로우 검증")
    void 포인트_충전_후_조회_통합_테스트() throws Exception {
        // Given - 사용자 ID
        long userId = 1L;
        long chargeAmount = 5000L;

        // When - 포인트 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount));

        // Then - 포인트 조회 시 충전된 금액 확인
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount));
    }

    @Test
    @DisplayName("포인트 충전, 사용, 내역 조회 - 복합 시나리오 검증")
    void 포인트_충전_사용_내역_조회_통합_테스트() throws Exception {
        // Given - 사용자 ID
        long userId = 2L;

        // When - 10000원 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(10000));

        // And - 3000원 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(7000));

        // Then - 잔액 확인
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(7000));

        // And - 내역 조회 (2건: 충전 1, 사용 1)
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].amount").value(10000))
                .andExpect(jsonPath("$[1].type").value("USE"))
                .andExpect(jsonPath("$[1].amount").value(3000));
    }

    @Test
    @DisplayName("잘못된 충전 단위 - 예외 처리 검증")
    void 잘못된_충전_단위_통합_테스트() throws Exception {
        // Given - 사용자 ID와 잘못된 충전 금액 (5000원 단위 아님)
        long userId = 3L;
        long invalidAmount = 3000L;

        // When & Then - 3000원 충전 시도 시 500 에러
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("잔액 부족 시 사용 불가 - 예외 처리 검증")
    void 잔액_부족_사용_통합_테스트() throws Exception {
        // Given - 사용자에게 5000원 충전
        long userId = 4L;
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("5000"));

        // When & Then - 10000원 사용 시도 시 500 에러
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("10000"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"));
    }

    @Test
    @DisplayName("포인트 내역 5건 제한 - 최근 5건만 반환")
    void 포인트_내역_5건_제한_통합_테스트() throws Exception {
        // Given - 사용자에게 7건의 거래 발생
        long userId = 5L;

        // 충전 5회 (5000, 10000, 15000, 20000, 25000원)
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(patch("/point/{id}/charge", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("5000"));
        }

        // 사용 2회 (1000원씩)
        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(patch("/point/{id}/use", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("1000"));
        }

        // When - 내역 조회
        // Then - 최근 5건만 반환되어야 함 (충전 3건 + 사용 2건)
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("최대 잔액 제한 - 100,000원 초과 불가")
    void 최대_잔액_제한_통합_테스트() throws Exception {
        // Given - 사용자에게 95000원 충전
        long userId = 6L;
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("95000"));

        // When & Then - 10000원 추가 충전 시도 시 500 에러 (총 105000원 > 100000원)
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("10000"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"));
    }

    @Test
    @DisplayName("최소 사용 금액 검증 - 500원 미만 사용 불가")
    void 최소_사용_금액_통합_테스트() throws Exception {
        // Given - 사용자에게 10000원 충전
        long userId = 7L;
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("10000"));

        // When & Then - 300원 사용 시도 시 500 에러
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("300"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"));
    }
}
