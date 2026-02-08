package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardUpdateRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.CardService;
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
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CardController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(TestSecurityConfig.class)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    private CustomUserDetails userDetails;
    private CardResponse cardResponse;
    private CreateCardRequest createCardRequest;
    private CardUpdateRequest cardUpdateRequest;
    private Long cardId;
    private Long userId;
    private String maskedCardNumber;
    private String holderName;

    private static final String CARDS_URL = "/api/cards";
    private static final String CARD_BY_ID_URL = "/api/cards/{id}";
    private static final String BLOCK_CARD_URL = "/api/cards/{id}/block";

    @BeforeEach
    void setUp() {
        cardId = 1L;
        userId = 1L;
        String cardNumber = "1111 2222 3333 4444";
        maskedCardNumber = "**** **** **** 4444";
        holderName = "IVAN IVANOV";

        User user = User.builder()
                .id(userId)
                .username("user")
                .email("email@example.com")
                .password("password")
                .roles(Set.of())
                .build();
        userDetails = new CustomUserDetails(user);

        createCardRequest = CreateCardRequest.builder()
                .cardNumber(cardNumber)
                .holderName(holderName)
                .expiryDate("12/30")
                .cvv("777")
                .build();

        cardUpdateRequest = CardUpdateRequest.builder()
                .status("ACTIVE")
                .build();

        cardResponse = CardResponse.builder()
                .id(cardId)
                .maskedCardNumber(maskedCardNumber)
                .holderName(holderName)
                .expiryDate("12/30")
                .status(CardStatus.ACTIVE.name())
                .balance(new BigDecimal("1000.00"))
                .build();
    }

    @Nested
    @DisplayName("POST " + CARDS_URL)
    class CreateCard {

        @Test
        @DisplayName("Should create card successfully")
        void shouldCreateCardSuccessfully() throws Exception {
            when(cardService.createCard(any(CreateCardRequest.class), eq(userId)))
                    .thenReturn(cardResponse);

            mockMvc.perform(post(CARDS_URL)
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createCardRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(cardId))
                    .andExpect(jsonPath("$.holderName").value(holderName))
                    .andExpect(jsonPath("$.maskedCardNumber").value(maskedCardNumber));

            verify(cardService).createCard(any(CreateCardRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        void shouldReturn400WhenValidationFails() throws Exception {
            CreateCardRequest invalidRequest = CreateCardRequest.builder()
                    .cardNumber("invalid")
                    .holderName("invalid")
                    .expiryDate("invalid")
                    .cvv("invalid")
                    .build();

            mockMvc.perform(post(CARDS_URL)
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(cardService, never()).createCard(any(), any());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthorized() throws Exception {
            mockMvc.perform(post(CARDS_URL))
                    .andExpect(status().isUnauthorized());

            verify(cardService, never()).createCard(any(), any());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(cardService.createCard(any(CreateCardRequest.class), eq(userId)))
                    .thenThrow(ResourceNotFoundException.user(userId));

            mockMvc.perform(post(CARDS_URL)
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createCardRequest)))
                    .andExpect(status().isNotFound());

            verify(cardService).createCard(any(CreateCardRequest.class), eq(userId));
        }
    }

    @Nested
    @DisplayName("GET " + CARDS_URL)
    class GetAllCards {

        @Test
        @DisplayName("Should return page of cards")
        void shouldReturnPageOfCards() throws Exception {
            Page<CardResponse> cardPage = new PageImpl<>(
                    List.of(cardResponse),
                    PageRequest.of(0, 10),
                    1
            );
            when(cardService.getCardsForUser(eq(userId), any(Pageable.class))).thenReturn(cardPage);

            mockMvc.perform(get(CARDS_URL)
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(cardId))
                    .andExpect(jsonPath("$.content[0].holderName").value(holderName))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(cardService).getCardsForUser(eq(userId), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page of cards")
        void shouldReturnEmptyPageOfCards() throws Exception {
            Page<CardResponse> emptyPage = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 10),
                    0
            );
            when(cardService.getCardsForUser(eq(userId), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get(CARDS_URL)
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));

            verify(cardService).getCardsForUser(eq(userId), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get(CARDS_URL))
                    .andExpect(status().isUnauthorized());

            verify(cardService, never()).getCardsForUser(any(), any());
        }
    }

    @Nested
    @DisplayName("GET " + CARD_BY_ID_URL)
    class GetCardById {

        @Test
        @DisplayName("Should return card successfully")
        @WithMockUser(roles = "USER")
        void shouldReturnCardSuccessfully() throws Exception {
            when(cardService.getCardById(cardId)).thenReturn(cardResponse);

            mockMvc.perform(get(CARD_BY_ID_URL, cardId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(cardId))
                    .andExpect(jsonPath("$.holderName").value(holderName))
                    .andExpect(jsonPath("$.maskedCardNumber").value(maskedCardNumber));

            verify(cardService).getCardById(cardId);
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get(CARD_BY_ID_URL, cardId))
                    .andExpect(status().isUnauthorized());

            verify(cardService, never()).getCardById(any());
        }

        @Test
        @DisplayName("Should return 404 when card not found")
        @WithMockUser(roles = "USER")
        void shouldReturn404WhenCardNotFound() throws Exception {
            when(cardService.getCardById(cardId)).thenThrow(ResourceNotFoundException.card(cardId));

            mockMvc.perform(get(CARD_BY_ID_URL, cardId))
                    .andExpect(status().isNotFound());

            verify(cardService).getCardById(cardId);
        }
    }

    @Nested
    @DisplayName("PUT " + CARD_BY_ID_URL)
    class UpdateCard {

        @Test
        @DisplayName("Should update card successfully")
        @WithMockUser(roles = "USER")
        void shouldUpdateCardSuccessfully() throws Exception {
            when(cardService.updateCard(eq(cardId), any(CardUpdateRequest.class))).thenReturn(cardResponse);

            mockMvc.perform(put(CARD_BY_ID_URL, cardId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cardUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(cardId))
                    .andExpect(jsonPath("$.status").value(CardStatus.ACTIVE.name()));

            verify(cardService).updateCard(eq(cardId), any(CardUpdateRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        @WithMockUser(roles = "USER")
        void shouldReturn400WhenValidationFails() throws Exception {
            CardUpdateRequest invalidRequest = CardUpdateRequest.builder()
                    .status("")
                    .build();

            mockMvc.perform(put(CARD_BY_ID_URL, cardId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(cardService, never()).updateCard(any(), any());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(put(CARD_BY_ID_URL, cardId))
                    .andExpect(status().isUnauthorized());

            verify(cardService, never()).updateCard(any(), any());
        }

        @Test
        @DisplayName("Should return 404 when card not found")
        @WithMockUser(roles = "USER")
        void shouldReturn404WhenCardNotFound() throws Exception {
            when(cardService.updateCard(eq(cardId), any(CardUpdateRequest.class)))
                    .thenThrow(ResourceNotFoundException.card(cardId));

            mockMvc.perform(put(CARD_BY_ID_URL, cardId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cardUpdateRequest)))
                    .andExpect(status().isNotFound());

            verify(cardService).updateCard(eq(cardId), any(CardUpdateRequest.class));
        }

        @Test
        @DisplayName("Should return 422 when card status invalid")
        @WithMockUser(roles = "USER")
        void shouldReturn422WhenCardStatusInvalid() throws Exception {
            CardUpdateRequest invalidStatusRequest = CardUpdateRequest.builder()
                    .status("INVALID_STATUS")
                    .build();
            when(cardService.updateCard(eq(cardId), any(CardUpdateRequest.class)))
                    .thenThrow(BusinessException.invalidCardStatus("INVALID_STATUS"));

            mockMvc.perform(put(CARD_BY_ID_URL, cardId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidStatusRequest)))
                    .andExpect(status().isUnprocessableEntity());

            verify(cardService).updateCard(eq(cardId), any(CardUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE " + CARD_BY_ID_URL)
    class DeleteCard {

        @Test
        @DisplayName("Should delete card successfully")
        @WithMockUser(roles = "USER")
        void shouldDeleteCardSuccessfully() throws Exception {
            doNothing().when(cardService).deleteCard(cardId);

            mockMvc.perform(delete(CARD_BY_ID_URL, cardId))
                    .andExpect(status().isNoContent());

            verify(cardService).deleteCard(cardId);
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(delete(CARD_BY_ID_URL, cardId))
                    .andExpect(status().isUnauthorized());

            verify(cardService, never()).deleteCard(any());
        }

        @Test
        @DisplayName("Should return 404 when card not found")
        @WithMockUser(roles = "USER")
        void shouldReturn404WhenCardNotFound() throws Exception {
            doThrow(ResourceNotFoundException.card(cardId)).when(cardService).deleteCard(cardId);

            mockMvc.perform(delete(CARD_BY_ID_URL, cardId))
                    .andExpect(status().isNotFound());

            verify(cardService).deleteCard(cardId);
        }
    }

    @Nested
    @DisplayName("PUT " + BLOCK_CARD_URL)
    class BlockCard {

        @Test
        @DisplayName("Should block card successfully")
        @WithMockUser(roles = "USER")
        void shouldBlockCardSuccessfully() throws Exception {
            CardResponse blockedResponse = CardResponse.builder()
                    .id(cardId)
                    .maskedCardNumber(maskedCardNumber)
                    .holderName(holderName)
                    .status(CardStatus.BLOCKED.name())
                    .build();
            when(cardService.blockCard(cardId)).thenReturn(blockedResponse);

            mockMvc.perform(put(BLOCK_CARD_URL, cardId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(cardId))
                    .andExpect(jsonPath("$.status").value(CardStatus.BLOCKED.name()));

            verify(cardService).blockCard(cardId);
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(put(BLOCK_CARD_URL, cardId))
                    .andExpect(status().isUnauthorized());

            verify(cardService, never()).blockCard(any());
        }

        @Test
        @DisplayName("Should return 404 when card not found")
        @WithMockUser(roles = "USER")
        void shouldReturn404WhenCardNotFound() throws Exception {
            when(cardService.blockCard(cardId)).thenThrow(ResourceNotFoundException.card(cardId));

            mockMvc.perform(put(BLOCK_CARD_URL, cardId))
                    .andExpect(status().isNotFound());

            verify(cardService).blockCard(cardId);
        }
    }
}
