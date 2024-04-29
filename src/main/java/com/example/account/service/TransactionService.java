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
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.account.type.ErrorCode.*;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    /**
     * 잔액사용
     * 사용자가 없는 경우
     * 계좌가 없는 경우
     * 실패 응답
     *
     * @param userId
     * @param accountNumber
     * @param amount
     * @return
     */
    @Transactional
    public TransactionDto useBalance(
            Long userId, String accountNumber, Long amount
    ) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateUseBalance(accountUser, account, amount);
        //이렇게 하는 것보다 Account 내부에 method를 생성하는 것이 더 좋다
//        Long accountBalance = account.getBalance();
//        account.setBalance(accountBalance - amount);
        account.useBalance(amount);


        return TransactionDto.fromEntity(
                saveAndGetTransaction(USE, S, amount, account)
        );
    }

    /**
     * 사용자 아이디와 계좌 소유주가 다른 경우
     * 계좌가 이미 해지 상태인 경우
     * 거래 금액이 잔액보다 큰 경우
     * 거래 금액이 너무 작거나 큰 경우 (Request에서 제한 걸어둠)
     * 실패 응답
     *
     * @param accountUser
     * @param account
     */
    private void validateUseBalance(AccountUser accountUser, Account account, Long amount) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UNMATCHED);
        }

        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() < amount) {
            throw new AccountException(AMOUNT_EXCEED_BALANCE);
        }
    }

    /**
     * 거래 실패 시
     *
     * @param accountNumber
     * @param amount
     */
    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(USE, F, amount, account);
    }

    public Transaction saveAndGetTransaction(TransactionType transactionType,
                                             TransactionResultType transactionResultType,
                                             Long amount, Account account) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    /**
     * 잔액사용 취소
     * 거래 아이디에 해당하는 거래가 없는 경우
     * 거래 금액과 거래 취소 금액이 다른 경우 (부분 취소 불가능 )
     * 계좌가 없는 경우
     * 거래와 계좌가 일치하지 않는 경우
     * 실패 응답
     *
     * @param transactionId
     * @param accountNumber
     * @param amount
     * @return
     */
    @Transactional
    public TransactionDto cancelBalance(
            String transactionId, String accountNumber, Long amount
    ) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateCancelUsedBalance(transaction, account, amount);
        //거래 취소된 금액을 다시 넣어줌
        account.cancelBalance(amount);

        return TransactionDto.fromEntity(
                saveAndGetTransaction(CANCEL, S, amount, account)
        );
    }

    private void validateCancelUsedBalance(Transaction transaction, Account account, Long amount) {
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(TRANSACTION_ACCOUNT_UNMATCHED);
        }

        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(CANCEL_MUST_FULLY);
        }
        //거래 시간이 1년 전이면
        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(TOO_OLD_ORDER_TO_CANCEL);
        }
    }

    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(CANCEL, F, amount, account);
    }

    public TransactionDto queryTransaction(String transactionId) {
        return TransactionDto.fromEntity(transactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND)));
    }
}
