package com.atproto.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RepositoryManagerTest {

    @Mock
    private CarFileHandler carFileHandler;

    @InjectMocks
    private RepositoryManager repositoryManager;

    private static final String TEST_REPO_ID = "did:plc:12345";
    private static final String TEST_RECORD_ID = "record123";

    @BeforeEach
    void setUp() {
        repositoryManager = new RepositoryManager();
    }

    @Test
    public void testRepositoryCreation() throws IOException {
        // Test creating a new repository
        assertTrue(repositoryManager.createRepository(TEST_REPO_ID));

        // Test creating repository with invalid DID
        assertFalse(repositoryManager.createRepository("invalid:did"));

        // Test creating repository that already exists
        when(carFileHandler.repositoryExists(TEST_REPO_ID)).thenReturn(true);
        assertFalse(repositoryManager.createRepository(TEST_REPO_ID));
    }

    @Test
    public void testRecordOperations() throws IOException {
        // Test adding a record
        Map<String, Object> recordData = new HashMap<>();
        recordData.put("text", "Test record");
        assertTrue(repositoryManager.addRecord(TEST_REPO_ID, TEST_RECORD_ID, recordData));

        // Test getting a record
        Map<String, Object> retrievedRecord = repositoryManager.getRecord(TEST_REPO_ID, TEST_RECORD_ID);
        assertNotNull(retrievedRecord);
        assertEquals("Test record", retrievedRecord.get("text"));

        // Test updating a record
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("text", "Updated record");
        assertTrue(repositoryManager.updateRecord(TEST_REPO_ID, TEST_RECORD_ID, updatedData));

        // Test getting updated record
        Map<String, Object> updatedRecord = repositoryManager.getRecord(TEST_REPO_ID, TEST_RECORD_ID);
        assertNotNull(updatedRecord);
        assertEquals("Updated record", updatedRecord.get("text"));

        // Test deleting a record
        assertTrue(repositoryManager.deleteRecord(TEST_REPO_ID, TEST_RECORD_ID));
        assertNull(repositoryManager.getRecord(TEST_REPO_ID, TEST_RECORD_ID));
    }

    @Test
    public void testRepositorySync() throws IOException {
        // Test syncing repository
        assertTrue(repositoryManager.syncRepository(TEST_REPO_ID));

        // Test syncing non-existent repository
        when(carFileHandler.repositoryExists("nonexistent:did")).thenReturn(false);
        assertFalse(repositoryManager.syncRepository("nonexistent:did"));

        // Test syncing with conflicts
        when(carFileHandler.hasConflicts(TEST_REPO_ID)).thenReturn(true);
        assertFalse(repositoryManager.syncRepository(TEST_REPO_ID));
    }

    @Test
    public void testErrorHandling() {
        // Test invalid DID format
        assertThrows(IllegalArgumentException.class, () -> {
            repositoryManager.createRepository("invalid:did");
        });

        // Test non-existent repository
        when(carFileHandler.repositoryExists(TEST_REPO_ID)).thenReturn(false);
        assertThrows(RepositoryNotFoundException.class, () -> {
            repositoryManager.getRecord(TEST_REPO_ID, TEST_RECORD_ID);
        });

        // Test record not found
        when(carFileHandler.recordExists(TEST_REPO_ID, TEST_RECORD_ID)).thenReturn(false);
        assertThrows(RecordNotFoundException.class, () -> {
            repositoryManager.getRecord(TEST_REPO_ID, TEST_RECORD_ID);
        });
    }
}
