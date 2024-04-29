package com.example.account.dto;

import com.example.account.domain.Account;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountInfo {
    private String accountNumber;
    private Long balance;

    public static AccountInfo from(Account account) {
        return AccountInfo.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .build();
    }
}
