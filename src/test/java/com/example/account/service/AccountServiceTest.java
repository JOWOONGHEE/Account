package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.dto.AccountInfo;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.ErrorCode.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;
    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {

        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000013").build()));

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015")
                        .build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when 어떤 경우에
        AccountDto accountDto = accountService.createAccount(1L, 1000L);
        //then 이런 결과가 나온다.
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals("1000000014", captor.getValue().getAccountNumber());
        assertEquals(12L, accountDto.getUserId());
    }

    @Test
    void createFirstAccount() {

        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015")
                        .build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when 어떤 경우에
        AccountDto accountDto = accountService.createAccount(1L, 1000L);
        //then 이런 결과가 나온다.
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
        assertEquals(15L, accountDto.getUserId());
    }

    @Test
    @DisplayName("No user - Failed Create Account")
    void createAccount_UserNotFound() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then 이런 결과가 나온다.
        assertEquals(USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("overMaxAccount - Failed Create Account")
    void createAccount_maxAccountIs10() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);
        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then 이런 결과가 나온다.
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, accountException.getErrorCode());
    }

    @Test
    void deleteAccount_success() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.ofNullable(Account.builder()
                        .accountUser(user)
                        .accountNumber("1234567890")
                        .balance(0L)
                        .build()));
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when 어떤 경우에
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567800");

        //then 이런 결과가 나온다.
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1234567890", accountDto.getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("No user - Failed Delete Account")
    void deleteAccount_UserNotFound() {
        //given 어떤 데이터가 주어졌을 때
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then 이런 결과가 나온다.
        assertEquals(USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("No Registered Account - Failed Delete Account")
    void deleteAccount_AccountNotFound() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then 이런 결과가 나온다.
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("User Account Unmatched - Failed Delete Account")
    void deleteAccount_User_Account_Unmatched() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(15L);
        //given 어떤 데이터가 주어졌을 때
        AccountUser user2 = AccountUser.builder()
                .name("Porong")
                .build();
        user2.setId(3L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user2)
                        .balance(0L)
                        .accountStatus(IN_USE)
                        .accountNumber("1234567890")
                        .build()));

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then 이런 결과가 나온다.
        assertEquals(ErrorCode.USER_ACCOUNT_UNMATCHED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("ACCOUNT ALREADY UNREGISTERED - Failed Delete Account")
    void deleteAccount_Account_Already_Unregistered() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountStatus(UNREGISTERED)
                        .accountNumber("1234567890")
                        .build()));

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then 이런 결과가 나온다.
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("BALANCE_NOT_EMPTY - Failed Delete Account")
    void deleteAccount_Balance_Not_Empty() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(100L)
                        .accountStatus(IN_USE)
                        .accountNumber("1234567890")
                        .build()));

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then 이런 결과가 나온다.
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("Success - getAccounts")
    void getAccountsByUserId_success() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .build();
        accountUser.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(accountUser));

        given(accountRepository.findByAccountUser(any()))
                .willReturn(List.of(
                        Account.builder()
                                .accountNumber("1234567890")
                                .balance(100L)
                                .build()
                ));

        //when
        List<AccountInfo> accountsByUserId = accountService.getAccountsByUserId(1L);
        //then
        assertEquals("1234567890", accountsByUserId.get(0).getAccountNumber());
        assertEquals(100L, accountsByUserId.get(0).getBalance());
    }

    @Test
    @DisplayName("Failed - USER_NOT_FOUND")
    void getAccountsByUserId_fail() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));
        //then
        assertEquals(USER_NOT_FOUND, accountException.getErrorCode());
    }

}