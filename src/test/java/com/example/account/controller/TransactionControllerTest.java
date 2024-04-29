package com.example.account.controller;

import com.example.account.dto.CancelBalance;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.USE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void useBalance_success() throws Exception {
        //given 어떤 데이터가 주어졌을 때
        given(transactionService.useBalance(
                anyLong(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1234567890")
                        .transactionType(USE)
                        .transactionResultType(S)
                        .amount(10000L)
                        .balanceSnapshot(1000L)
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build());

        //when 어떤 경우에
        //then 이런 결과가 나온다.
        mockMvc.perform(post("/transaction/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UseBalance.Request(333L
                                        , "1234567890"
                                        , 1000L)
                        )))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber")
                        .value("1234567890"))
                .andExpect(jsonPath("$.transactionResult")
                        .value("S"))
                .andExpect(jsonPath("$.amount")
                        .value("10000"));
    }

    @Test
    void cancelBalance_success() throws Exception {
        //given 어떤 데이터가 주어졌을 때
        given(transactionService.cancelBalance(anyString(), anyString()
                , anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1234567890")
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .transactionResultType(S)
                        .build());
        //when 어떤 경우에
        //then 이런 결과가 나온다.
        mockMvc.perform(post("/transaction/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancelBalance.Request("transaction"
                                        , "1234567800"
                                        , 10L)
                        )))
                .andDo(print())
                .andExpect(jsonPath("$.amount")
                        .value(1000L))
                .andExpect(jsonPath("$.accountNumber")
                        .value("1234567890"));
    }

    @Test
    void queryTransaction() throws Exception {
        //given 어떤 데이터가 주어졌을 때
        given(transactionService.queryTransaction(anyString()))
                .willReturn(TransactionDto.builder()
                        .transactionId("transactionId")
                        .transactionType(USE)
                        .transactionResultType(S)
                        .accountNumber("1234567890")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .build());
        //when 어떤 경우에
        //then 이런 결과가 나온다.
        mockMvc.perform(get("/transaction?transactionId=transactionId"))
                .andDo(print())
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.transactionType").value("USE"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.amount").value(1000));
    }

}