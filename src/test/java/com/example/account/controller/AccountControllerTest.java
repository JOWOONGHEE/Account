package com.example.account.controller;

import com.example.account.dto.AccountDto;
import com.example.account.dto.AccountInfo;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.exception.AccountException;
import com.example.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.account.type.ErrorCode.ACCOUNT_NOT_FOUND;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {
    @MockBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successCreateAccount() throws Exception {
        //given
        given(accountService.createAccount(anyLong(), anyLong()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        //when
        //then
        mockMvc.perform(post("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        //1. 하기와 같이 문자열을 그대로 넣는 방법
//                .content("{\n" +
//                        "    \"userId\" : 1,\n" +
//                        "    \"initialBalance\" : 100\n" +
//                        "}")

                        //2. objectMapper를 이용한 방법
                        .content(objectMapper.writeValueAsString(
                                new CreateAccount.Request(333L, 100L)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("1"))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }

    @Test
    void successDeleteAccount() throws Exception {
        //given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        //when
        //then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        //1. 하기와 같이 문자열을 그대로 넣는 방법
//                .content("{\n" +
//                        "    \"userId\" : 1,\n" +
//                        "    \"initialBalance\" : 100\n" +
//                        "}")

                        //2. objectMapper를 이용한 방법
                        .content(objectMapper.writeValueAsString(
                                new DeleteAccount.Request(333L, "1234567890")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("1"))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }

    @Test
    void successGetAccountsByUserId() throws Exception {
        //given
        given(accountService.getAccountsByUserId(anyLong()))
                .willReturn(List.of(AccountInfo.builder()
                                .balance(100L)
                                .accountNumber("1234567890")
                                .build(),
                        AccountInfo.builder()
                                .balance(1000L)
                                .accountNumber("1234567800")
                                .build(),
                        AccountInfo.builder()
                                .balance(10000L)
                                .accountNumber("1234567000")
                                .build()));

        //when
        //then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$[0].accountNumber").value("1234567890"))
                .andExpect(jsonPath("$[0].balance").value("100"))
                .andExpect(jsonPath("$[1].accountNumber").value("1234567800"))
                .andExpect(jsonPath("$[1].balance").value("1000"))
                .andExpect(jsonPath("$[2].accountNumber").value("1234567000"))
                .andExpect(jsonPath("$[2].balance").value("10000"))
                .andExpect(status().isOk());
    }

    @Test
    void failGetAccount() throws Exception {
        //given
        given(accountService.getAccountsByUserId(anyLong()))
                .willThrow(new AccountException(ACCOUNT_NOT_FOUND));

        //when
        //then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value(ACCOUNT_NOT_FOUND.getDescription()));
    }
}