package com.atproto.concurrency;

import com.atproto.repository.RepositoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.assertj.core.api.Assertions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConcurrentRepositoryAccessTest {
    private static final int THREAD_COUNT = 20;
    private static final int ITERATIONS = 100;
    
    @Mock
    private RepositoryManager repositoryManager;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 20, 30})
    @Timeout(30)
    public void testConcurrentRepositoryAccess(int threadCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        testRepositoryOperations(repositoryManager);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            
            // Verify using AssertJ for better assertions
            Assertions.assertThat(repositoryManager)
                .isNotNull()
                .hasFieldOrProperty("repositoryState");
            
            verify(repositoryManager, times(threadCount * ITERATIONS)).getRepositoryState();
            verify(repositoryManager, times(threadCount * ITERATIONS)).updateRepositoryState();
            verify(repositoryManager, times(threadCount * ITERATIONS)).uploadBlob();
            verify(repositoryManager, times(threadCount * ITERATIONS)).downloadBlob();
        } finally {
            executor.shutdown();
        }
    }

    private void testRepositoryOperations(RepositoryManager repositoryManager) {
        for (int i = 0; i < ITERATIONS; i++) {
            // Test concurrent read operations
            repositoryManager.getRepositoryState();
            
            // Test concurrent write operations
            repositoryManager.updateRepositoryState();
            
            // Test concurrent blob operations
            repositoryManager.uploadBlob();
            repositoryManager.downloadBlob();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 20, 30})
    @Timeout(30)
    public void testRepositorySyncConcurrency(int threadCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        testRepositorySyncOperations(repositoryManager);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            
            // Verify using AssertJ for better assertions
            Assertions.assertThat(repositoryManager)
                .isNotNull()
                .hasFieldOrProperty("commitHistory");
            
            verify(repositoryManager, times(threadCount * ITERATIONS)).syncRepository();
            verify(repositoryManager, times(threadCount * ITERATIONS)).createCommit();
            verify(repositoryManager, times(threadCount * ITERATIONS)).getLatestCommit();
        } finally {
            executor.shutdown();
        }
    }

    private void testRepositorySyncOperations(RepositoryManager repositoryManager) {
        for (int i = 0; i < ITERATIONS; i++) {
            // Test concurrent repository synchronization
            repositoryManager.syncRepository();
            
            // Test concurrent commit operations
            repositoryManager.createCommit();
            repositoryManager.getLatestCommit();
        }
    }
}
