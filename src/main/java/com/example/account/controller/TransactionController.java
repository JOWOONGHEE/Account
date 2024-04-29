package com.example.account.controller;

import com.example.account.aop.AccountLock;
import com.example.account.dto.CancelBalance;
import com.example.account.dto.QueryTransactionResponse;
import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 잔액 관련 컨트롤러
 * 1. 잔액 사용
 * 2. 잔액 사용 취소
 * 3. 거래 확인
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    /**
     * 잔액 사용
     * @param request
     * @return
     */
    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(
            @RequestBody @Valid UseBalance.Request request
    ) throws InterruptedException {
        try {
            //AOP LOCK이 잘 작동하는 지 확인하기 위해 넣음
            Thread.sleep(3000L);

            return UseBalance.Response.from(transactionService.useBalance(
                            request.getUserId()
                            , request.getAccountNumber()
                            , request.getAmount()
                    )
            );
        } catch (AccountException ae) {
            log.error("Failed to use balance");
            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw ae;
        }
    }

    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(
            @RequestBody @Valid CancelBalance.Request request
    ) {
        try {
            return CancelBalance.Response.from(transactionService.cancelBalance(
                    request.getTransactionId()
                    , request.getAccountNumber()
                    , request.getAmount()
            ));
        } catch (AccountException ae) {
            log.error("Failed to cancel used balance");
            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw ae;
        }
    }

    @GetMapping("/transaction")
    public QueryTransactionResponse queryTransaction(
            @RequestParam("transactionId") String transactionId
    ) {
        return QueryTransactionResponse.from(
                transactionService.queryTransaction(transactionId)
        );
    }
}
