package com.example.account.service;

import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private LockService lockService;

    @Test
    void successGetLock() throws InterruptedException {
        //given 어떤 데이터가 주어졌을 때
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(true);
        //when 어떤 경우에 
        //true  던지고 아무 반응 없기때문에 하기와 같이 표현
        assertDoesNotThrow(() -> lockService.lock("1234567890"));

        //then 이런 결과가 나온다.
    }

    @Test
    void failGetLock() throws InterruptedException {
        //given 어떤 데이터가 주어졌을 때
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(false);
        //when 어떤 경우에
        AccountException accountException = assertThrows(AccountException.class
                , () -> lockService.lock("1234")
        );
        //then 이런 결과가 나온다.
        assertEquals(ErrorCode.ACCOUNT_TRANSACTION_LOCK, accountException.getErrorCode());
    }

}