package com.mingzhe.resumetailor.job;

import com.mingzhe.resumetailor.redis.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobImportDeduplicationServiceTest {

    @Mock
    private RedisCacheService redisCacheService;

    @Test
    void acquiresDeduplicationKeyAtomicallyForOneMinute() {
        when(redisCacheService.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(true);
        JobImportDeduplicationService service =
                new JobImportDeduplicationService(redisCacheService);

        assertTrue(service.tryAcquire(7L, "OpenAI", "Software Engineer"));

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redisCacheService).setIfAbsent(anyString(), eq("1"), ttlCaptor.capture());
        assertEquals(Duration.ofMinutes(1), ttlCaptor.getValue());
    }

    @Test
    void rejectsWhenTheSameKeyAlreadyExists() {
        when(redisCacheService.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(false);
        JobImportDeduplicationService service =
                new JobImportDeduplicationService(redisCacheService);

        assertFalse(service.tryAcquire(7L, "OpenAI", "Software Engineer"));
    }

    @Test
    void normalizesCaseAndWhitespaceForCompanyAndTitle() {
        when(redisCacheService.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(true);
        JobImportDeduplicationService service =
                new JobImportDeduplicationService(redisCacheService);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        service.tryAcquire(7L, " OpenAI ", "Software   Engineer");
        service.tryAcquire(7L, "openai", " software engineer ");

        verify(redisCacheService, org.mockito.Mockito.times(2))
                .setIfAbsent(keyCaptor.capture(), eq("1"), any(Duration.class));
        assertEquals(keyCaptor.getAllValues().get(0), keyCaptor.getAllValues().get(1));
    }

    @Test
    void keepsDifferentUsersAndJobCombinationsIndependent() {
        when(redisCacheService.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(true);
        JobImportDeduplicationService service =
                new JobImportDeduplicationService(redisCacheService);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        service.tryAcquire(7L, "OpenAI", "Software Engineer");
        service.tryAcquire(8L, "OpenAI", "Software Engineer");
        service.tryAcquire(7L, "OpenAI", "Product Manager");

        verify(redisCacheService, org.mockito.Mockito.times(3))
                .setIfAbsent(keyCaptor.capture(), eq("1"), any(Duration.class));
        assertNotEquals(keyCaptor.getAllValues().get(0), keyCaptor.getAllValues().get(1));
        assertNotEquals(keyCaptor.getAllValues().get(0), keyCaptor.getAllValues().get(2));
    }
}
