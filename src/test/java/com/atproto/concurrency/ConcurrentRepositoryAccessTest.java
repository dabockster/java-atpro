package com.atproto.concurrency;

import com.atproto.repository.RepositoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ConcurrentRepositoryAccessTest {
    private static final int THREAD_COUNT = 20;
    private static final int ITERATIONS = 100;
    
    @Mock
    private RepositoryManager repositoryManager;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Timeout(30)
    public void testConcurrentRepositoryAccess() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        testRepositoryOperations(repositoryManager);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            
            // Verify that all operations were called the expected number of times
            verify(repositoryManager, times(THREAD_COUNT * ITERATIONS)).getRepositoryState();
            verify(repositoryManager, times(THREAD_COUNT * ITERATIONS)).updateRepositoryState();
            verify(repositoryManager, times(THREAD_COUNT * ITERATIONS)).uploadBlob();
            verify(repositoryManager, times(THREAD_COUNT * ITERATIONS)).downloadBlob();
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

    @Test
    @Timeout(30)
    public void testRepositorySyncConcurrency() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        testRepositorySyncOperations(repositoryManager);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            
            // Verify that all operations were called the expected number of times
            verify(repositoryManager, times(THREAD_COUNT * ITERATIONS)).syncRepository();
            verify(repositoryManager, times(THREAD_COUNT * ITERATIONS)).createCommit();
            verify(repositoryManager, times(THREAD_COUNT * ITERATIONS)).getLatestCommit();
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
