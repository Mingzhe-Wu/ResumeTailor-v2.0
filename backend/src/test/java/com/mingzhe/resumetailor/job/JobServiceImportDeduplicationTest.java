package com.mingzhe.resumetailor.job;

import com.mingzhe.resumetailor.exceptions.TooManyRequestsException;
import com.mingzhe.resumetailor.resume.ResumeMapper;
import com.mingzhe.resumetailor.user.User;
import com.mingzhe.resumetailor.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImportDeduplicationTest {

    @Mock
    private JobMapper jobMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private JobImportDeduplicationService jobImportDeduplicationService;

    private JobService jobService;
    private ImportJobDTO request;
    private User user;

    @BeforeEach
    void setUp() {
        jobService = new JobService(
                jobMapper,
                userMapper,
                resumeMapper,
                jobImportDeduplicationService
        );

        user = new User();
        user.setId(12L);
        user.setEmail("user@example.com");
        when(userMapper.findByEmail("user@example.com")).thenReturn(user);

        request = new ImportJobDTO();
        request.setTitle(" Software Engineer ");
        request.setCompany(" OpenAI ");
        request.setDescription("Build reliable systems.");
    }

    @Test
    void importsJobAfterAcquiringDeduplicationKey() {
        when(jobImportDeduplicationService.tryAcquire(
                12L,
                "OpenAI",
                "Software Engineer"
        )).thenReturn(true);
        when(userMapper.findById(12L)).thenReturn(user);
        when(jobMapper.insert(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            job.setId(42L);
            return 1;
        });

        Job imported = jobService.importJobForAuthenticatedUser(request, "user@example.com");

        assertEquals(42L, imported.getId());
        verify(jobImportDeduplicationService).tryAcquire(
                12L,
                "OpenAI",
                "Software Engineer"
        );
        verify(jobImportDeduplicationService, never())
                .release(any(), any(), any());
    }

    @Test
    void rejectsDuplicateImportWithoutWritingJob() {
        when(jobImportDeduplicationService.tryAcquire(
                12L,
                "OpenAI",
                "Software Engineer"
        )).thenReturn(false);

        assertThrows(
                TooManyRequestsException.class,
                () -> jobService.importJobForAuthenticatedUser(request, "user@example.com")
        );

        verify(jobMapper, never()).insert(any(Job.class));
    }

    @Test
    void releasesDeduplicationKeyWhenJobCreationFails() {
        when(jobImportDeduplicationService.tryAcquire(
                12L,
                "OpenAI",
                "Software Engineer"
        )).thenReturn(true);
        when(userMapper.findById(12L)).thenReturn(user);
        when(jobMapper.insert(any(Job.class)))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> jobService.importJobForAuthenticatedUser(request, "user@example.com")
        );

        verify(jobImportDeduplicationService).release(
                12L,
                "OpenAI",
                "Software Engineer"
        );
    }
}
