package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.dto.AccountInfo;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     * 사용자가 있는 지 조회
     * 계좌의 번호를 생성하고
     * 계좌를 저장하고 그 정보를 넘긴다.
     *
     * @param userId
     * @param initialBalance
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) throws AccountException {
        AccountUser accountUser = getAccountUser(userId);

        validateCreateAccount(accountUser);

        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber()) + 1) + "")
                .orElse("1000000000");

        return AccountDto.fromEntity(
                accountRepository.save(Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .accountNumber(newAccountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build()));
    }

    /**
     * 사용자가 있는 지 조회
     * 계좌의 번호를 삭제한다.
     * 해지된 사용자와 계지번호 해지일자를 Return 한다.
     *
     * @param userId
     * @param accountNumber
     */
    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) throws AccountException {
        AccountUser accountUser = getAccountUser(userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        //테스트를 위해 넣은 save 없어도 상관 없다.
        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    private AccountUser getAccountUser(Long userId) {
        return accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
    }

    /**
     * 계좌 해지 시 유효성 검사 하기의 3가지 경우엔 해지 불가
     * 1. 계좌 소유주 아이디와 해지 시도한 아이디가 불일치 시
     * 2. 계좌 상태가 이미 해지일 시
     * 3. 남은 계좌의 잔액이 0원보다 클 경우
     *
     * @param accountUser
     * @param account
     */
    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UNMATCHED);
        }

        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    /**
     * 계좌 생성 시 유효성 검사 ( 한 사람이 가진 계좌가 10개가 넘는다면 생성 x)
     *
     * @param accountUser
     */

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) == 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
    }


    /**
     * 계좌의 정보를 가져온다.
     *
     * @param userId
     * @return 계좌 정보
     */
    public List<AccountInfo> getAccountsByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);

        List<Account> accounts = accountRepository.findByAccountUser(accountUser);


        return accounts.stream()
                .map(AccountInfo::from)
                .collect(Collectors.toList());
    }
}
