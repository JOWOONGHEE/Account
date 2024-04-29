package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.ErrorCode.*;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void success_useBalance() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .accountNumber("1234567890")
                        .accountStatus(AccountStatus.IN_USE)
                        .balance(30000L)
                        .build()));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(Account.builder()
                                .accountUser(user)
                                .accountNumber("1234567890")
                                .accountStatus(AccountStatus.IN_USE)
                                .balance(30000L)
                                .build())
                        .amount(10000L)
                        .balanceSnapshot(20000L)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when 어떤 경우에
        TransactionDto transactionDto = transactionService.useBalance(1L, "1234567890", 20000L);
        //then 이런 결과가 나온다.
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(20000L, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals("1234567890", transactionDto.getAccountNumber());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(20000L, transactionDto.getBalanceSnapshot());
        assertEquals(10000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("No user - Failed use Balance")
    void useBalance_UserNotFound() {
        //given 어떤 데이터가 주어졌을 때
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        //then 이런 결과가 나온다.
        assertEquals(USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("No Account - Failed use Balance")
    void useBalance_AccountNotFound() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        //then 이런 결과가 나온다.
        assertEquals(ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("Deferent id - Failed use Balance")
    void useBalance_userUnMatch() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(1L);
        AccountUser harry = AccountUser.builder()
                .name("Harry")
                .build();
        harry.setId(3L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry)
                        .balance(200L)
                        .accountNumber("1234567890")
                        .build()));

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 100L));

        //then 이런 결과가 나온다.
        assertEquals(USER_ACCOUNT_UNMATCHED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("AlreadyUnregistered - Failed use Balance")
    void useBalance_alreadyUnregistered() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(200L)
                        .accountNumber("1234567890")
                        .build()));

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 100L));

        //then 이런 결과가 나온다.
        assertEquals(ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("exceed Balance - Failed use Balance")
    void useBalance_exceed_Balance() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .accountStatus(AccountStatus.IN_USE)
                        .balance(200L)
                        .accountNumber("1234567890")
                        .build()));

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        //then 이런 결과가 나온다.
        assertEquals(AMOUNT_EXCEED_BALANCE, accountException.getErrorCode());
    }

    @Test
    @DisplayName("saveFailedUseTransaction")
    void saveFailedUseTransaction() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(1L);

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .accountStatus(AccountStatus.IN_USE)
                        .balance(200L)
                        .accountNumber("1234567890")
                        .build()));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(Account.builder()
                                .accountUser(user)
                                .accountNumber("1234567890")
                                .accountStatus(AccountStatus.IN_USE)
                                .balance(30000L)
                                .build())
                        .amount(10000L)
                        .balanceSnapshot(20000L)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        //when 어떤 경우에
        transactionService.saveFailedUseTransaction("1000000000", 1000L);
        //then 이런 결과가 나온다.
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(200L, captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

    @Test
    void success_cancelBalance() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(1L);

        Account account = Account.builder()
                .accountUser(user)
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(30000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.ofNullable(Transaction.builder()
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .transactionType(USE)
                        .transactionResultType(S)
                        .amount(10000L)
                        .account(account)
                        .build()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .amount(10000L)
                        .balanceSnapshot(40000L)
                        .transactionType(CANCEL)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when 어떤 경우에
        TransactionDto transactionDto = transactionService.cancelBalance("transactionId", "1234567890", 10000L);
        //then 이런 결과가 나온다.
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(10000L, captor.getValue().getAmount());
        assertEquals(40000L, captor.getValue().getBalanceSnapshot());
        assertEquals("1234567890", transactionDto.getAccountNumber());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(40000L, transactionDto.getBalanceSnapshot());
        assertEquals(10000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("No Transaction - Failed cancel Balance")
    void cancelBalance_TransactionIdNotFound() {
        //given 어떤 데이터가 주어졌을 때
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", 1000L));

        //then 이런 결과가 나온다.
        assertEquals(TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("No Account - Failed cancel Balance")
    void cancelBalance_AccountNotFound() {
        //given 어떤 데이터가 주어졌을 때
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.ofNullable(Transaction.builder()
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .transactionType(USE)
                        .transactionResultType(S)
                        .amount(10000L)
                        .build()));

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", 1000L));

        //then 이런 결과가 나온다.
        assertEquals(ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("TRANSACTION_ACCOUNT_UNMATCHED - Failed cancel Balance")
    void cancelBalance_unMatch() {
        //given 어떤 데이터가 주어졌을 때

        Account account = Account.builder()
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(30000L)
                .build();
        account.setId(1L);

        Account account2 = Account.builder()
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(30000L)
                .build();
        account2.setId(3L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .transactionType(USE)
                .transactionResultType(S)
                .amount(10000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.ofNullable(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.ofNullable(account2));

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", 1000L));

        //then 이런 결과가 나온다.
        assertEquals(TRANSACTION_ACCOUNT_UNMATCHED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("CANCEL_MUST_FULLY - Failed cancel Balance")
    void cancelBalance_CANCEL_MUST_FULLY() {
        //given 어떤 데이터가 주어졌을 때

        Account account = Account.builder()
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(30000L)
                .build();
        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .transactionType(USE)
                .transactionResultType(S)
                .amount(10000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.ofNullable(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.ofNullable(account));

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", 1000L));

        //then 이런 결과가 나온다.
        assertEquals(CANCEL_MUST_FULLY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("TOO_OLD_ORDER_TO_CANCEL - Failed cancel Balance")
    void cancelBalance_TOO_OLD_ORDER_TO_CANCEL() {
        //given 어떤 데이터가 주어졌을 때

        Account account = Account.builder()
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(30000L)
                .build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(2))
                .transactionType(USE)
                .transactionResultType(S)
                .amount(1000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.ofNullable(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.ofNullable(account));

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId"
                        , "1234567890"
                        , 1000L));

        //then 이런 결과가 나온다.
        assertEquals(TOO_OLD_ORDER_TO_CANCEL, accountException.getErrorCode());
    }

    @Test
    @DisplayName("saveFailedCancelTransaction")
    void saveFailedCancelTransaction() {
        //given 어떤 데이터가 주어졌을 때
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(1L);
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .accountStatus(AccountStatus.IN_USE)
                        .balance(200L)
                        .accountNumber("1234567890")
                        .build()));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(Account.builder()
                                .accountUser(user)
                                .accountNumber("1234567890")
                                .accountStatus(AccountStatus.IN_USE)
                                .balance(30000L)
                                .build())
                        .amount(10000L)
                        .balanceSnapshot(20000L)
                        .transactionType(CANCEL)
                        .transactionResultType(F)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        //when 어떤 경우에
        transactionService.saveFailedUseTransaction("1000000000", 10000L);
        //then 이런 결과가 나온다.
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(10000L, captor.getValue().getAmount());
        assertEquals(200L, captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

    @Test
    void queryTransaction() {
        //given 어떤 데이터가 주어졌을 때
        Account account = Account.builder()
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(30000L)
                .build();
        account.setId(1L);
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.ofNullable(Transaction.builder()
                        .account(account)
                        .transactionId("transactionId")
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .build()));
        //when 어떤 경우에
        TransactionDto transactionDto = transactionService.queryTransaction("transaction");
        //then 이런 결과가 나온다.
        assertEquals("transactionId", transactionDto.getTransactionId());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("No Transaction - Failed cancel Balance")
    void queryTransaction_TransactionIdNotFound() {
        //given 어떤 데이터가 주어졌을 때
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        //then 이런 결과가 나온다.
        assertEquals(TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }


}