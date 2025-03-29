package com.atproto.concurrency;

import com.atproto.repository.RepositoryManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentRepositoryAccessTest {
    private static final int THREAD_COUNT = 20;  // Based on Go implementation's workerCount
    private static final int ITERATIONS = 100;  // Based on Go's backfill parallel record creates

    @Test
    @Timeout(30)
    public void testConcurrentRepositoryAccess() throws InterruptedException {
        RepositoryManager repositoryManager = new RepositoryManager();
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        try {
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            
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
        RepositoryManager repositoryManager = new RepositoryManager();
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        try {
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            
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
