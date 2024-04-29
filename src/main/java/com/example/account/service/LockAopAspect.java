package com.example.account.service;

import com.example.account.aop.AccountLockIdInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {
    private final LockService lockService;

    //어떤 경우에 이 Aspect를 적용할 것인지
    @Around("@annotation(com.example.account.aop.AccountLock) && args(request)")
    public Object aroundMethod(
            ProceedingJoinPoint pjp,
            AccountLockIdInterface request
    ) throws Throwable {
        //lock 취득 시도
        lockService.lock(request.getAccountNumber());
        try {
            //Around로 지정했기 때문에 before ,After에 모두 적용된다.
            return pjp.proceed();
        } finally {
            //lock 해제
//            pjp.st
            lockService.unlock(request.getAccountNumber());
        }
    }
}
