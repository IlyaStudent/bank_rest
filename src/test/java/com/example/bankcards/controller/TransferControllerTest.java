package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TransferController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(TestSecurityConfig.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

    private CustomUserDetails userDetails;
    private TransferResponse transferResponse;
    private TransferRequest transferRequest;
    private Long userId;
    private Long transferResponseId;
    private Long destinationCardId;
    private BigDecimal transferAmount;
    private Page<TransferResponse> transferResponsePage;

    private static final Instant FIXED_TIMESTAMP = Instant.parse("2026-01-01T12:00:00Z");

    private static final String TRANSFERS_URL = "/api/transfers";

    @BeforeEach
    void setUp() {
        userId = 1L;
        transferResponseId = 1L;
        destinationCardId = 2L;
        transferAmount = new BigDecimal("100.00");

        User user = User.builder()
                .id(userId)
                .username("user")
                .email("email@example.com")
                .password("password")
                .roles(Set.of())
                .build();
        userDetails = new CustomUserDetails(user);

        transferRequest = TransferRequest.builder()
                .sourceCardId(1L)
                .destinationCardId(destinationCardId)
                .amount(transferAmount)
                .description("Test transfer")
                .build();

        transferResponse = TransferResponse.builder()
                .id(transferResponseId)
                .sourceCardMasked("**** **** **** 4444")
                .destinationCardMasked("**** **** **** 8888")
                .amount(transferAmount)
                .timestamp(FIXED_TIMESTAMP)
                .status(TransferStatus.SUCCESS.name())
                .build();

        transferResponsePage = new PageImpl<>(
                List.of(transferResponse),
                PageRequest.of(0, 10),
                1
        );
    }

    @Nested
    @DisplayName("POST " + TRANSFERS_URL)
    class TransferMoney {

        @Test
        @DisplayName("Should create transfer successfully")
        @WithMockUser(roles = "USER")
        void shouldCreateTransferSuccessfully() throws Exception {
            when(transferService.transferMoney(any(TransferRequest.class))).thenReturn(transferResponse);

            mockMvc.perform(post(TRANSFERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transferRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(transferResponseId))
                    .andExpect(jsonPath("$.amount").value(transferAmount.doubleValue()));

            verify(transferService).transferMoney(any(TransferRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        @WithMockUser(roles = "USER")
        void shouldReturn400WhenValidationFails() throws Exception {
            TransferRequest invalidRequest = TransferRequest.builder()
                    .sourceCardId(1L)
                    .destinationCardId(2L)
                    .amount(new BigDecimal("-100.00"))
                    .description("")
                    .build();

            mockMvc.perform(post(TRANSFERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(transferService, never()).transferMoney(any());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post(TRANSFERS_URL))
                    .andExpect(status().isUnauthorized());

            verify(transferService, never()).transferMoney(any());
        }

        @Test
        @DisplayName("Should return 404 when card not found")
        @WithMockUser(roles = "USER")
        void shouldReturn404WhenCardNotFound() throws Exception {
            when(transferService.transferMoney(any(TransferRequest.class)))
                    .thenThrow(ResourceNotFoundException.card(destinationCardId));

            mockMvc.perform(post(TRANSFERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transferRequest)))
                    .andExpect(status().isNotFound());

            verify(transferService).transferMoney(any(TransferRequest.class));
        }

        @Test
        @DisplayName("Should return 422 when same card transfer")
        @WithMockUser(roles = "USER")
        void shouldReturn422WhenSameCardTransfer() throws Exception {
            TransferRequest invalidRequest = TransferRequest.builder()
                    .sourceCardId(1L)
                    .destinationCardId(1L)
                    .amount(new BigDecimal("100.00"))
                    .description("")
                    .build();
            when(transferService.transferMoney(any(TransferRequest.class)))
                    .thenThrow(BusinessException.sameCardTransfer());

            mockMvc.perform(post(TRANSFERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isUnprocessableEntity());

            verify(transferService).transferMoney(any(TransferRequest.class));
        }

        @Test
        @DisplayName("Should return 422 when insufficient funds")
        @WithMockUser(roles = "USER")
        void shouldReturn422WhenInsufficientFunds() throws Exception {
            when(transferService.transferMoney(any(TransferRequest.class)))
                    .thenThrow(BusinessException.insufficientFunds(transferRequest.getAmount(), BigDecimal.ZERO));

            mockMvc.perform(post(TRANSFERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transferRequest)))
                    .andExpect(status().isUnprocessableEntity());

            verify(transferService).transferMoney(any(TransferRequest.class));
        }
    }

    @Nested
    @DisplayName("GET " + TRANSFERS_URL)
    class GetTransferHistory {

        @Test
        @DisplayName("Should return page of transfers")
        void shouldReturnPageOfTransfers() throws Exception {
            when(transferService.getTransferHistory(eq(userId), any(Pageable.class))).thenReturn(transferResponsePage);

            mockMvc.perform(get(TRANSFERS_URL)
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(transferResponseId))
                    .andExpect(jsonPath("$.content[0].amount").value(transferAmount.doubleValue()))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(transferService).getTransferHistory(eq(userId), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page of transfers")
        void shouldReturnEmptyPageOfTransfers() throws Exception {
            Page<TransferResponse> emptyPage = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 10),
                    0
            );
            when(transferService.getTransferHistory(eq(userId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            mockMvc.perform(get(TRANSFERS_URL)
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));

            verify(transferService).getTransferHistory(eq(userId), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get(TRANSFERS_URL))
                    .andExpect(status().isUnauthorized());

            verify(transferService, never()).getTransferHistory(any(), any());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(transferService.getTransferHistory(eq(userId), any(Pageable.class)))
                    .thenThrow(ResourceNotFoundException.user(userId));

            mockMvc.perform(get(TRANSFERS_URL)
                            .with(user(userDetails)))
                    .andExpect(status().isNotFound());

            verify(transferService).getTransferHistory(eq(userId), any(Pageable.class));
        }

    }
}
