package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
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
}
