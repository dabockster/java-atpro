package com.atproto.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        when(carFileHandler.repositoryExists(TEST_REPO_ID)).thenReturn(false);
        assertThat(repositoryManager.createRepository(TEST_REPO_ID)).isTrue();

        // Test creating repository with invalid DID
        assertThatThrownBy(() -> repositoryManager.createRepository("invalid:did"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid DID format");

        // Test creating repository that already exists
        when(carFileHandler.repositoryExists(TEST_REPO_ID)).thenReturn(true);
        assertThat(repositoryManager.createRepository(TEST_REPO_ID)).isFalse();
    }

    @Test
    public void testRecordOperations() throws IOException {
        // Test adding a record
        Map<String, Object> recordData = new HashMap<>();
        recordData.put("text", "Test record");
        
        when(carFileHandler.addRecord(TEST_REPO_ID, TEST_RECORD_ID, recordData)).thenReturn(true);
        assertThat(repositoryManager.addRecord(TEST_REPO_ID, TEST_RECORD_ID, recordData)).isTrue();

        // Test getting a record
        when(carFileHandler.getRecord(TEST_REPO_ID, TEST_RECORD_ID)).thenReturn(recordData);
        Map<String, Object> retrievedRecord = repositoryManager.getRecord(TEST_REPO_ID, TEST_RECORD_ID);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.get("text")).isEqualTo("Test record");

        // Test updating a record
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("text", "Updated record");
        
        when(carFileHandler.updateRecord(TEST_REPO_ID, TEST_RECORD_ID, updatedData)).thenReturn(true);
        assertThat(repositoryManager.updateRecord(TEST_REPO_ID, TEST_RECORD_ID, updatedData)).isTrue();

        // Test getting updated record
        when(carFileHandler.getRecord(TEST_REPO_ID, TEST_RECORD_ID)).thenReturn(updatedData);
        Map<String, Object> updatedRecord = repositoryManager.getRecord(TEST_REPO_ID, TEST_RECORD_ID);
        assertThat(updatedRecord).isNotNull();
        assertThat(updatedRecord.get("text")).isEqualTo("Updated record");

        // Test deleting a record
        when(carFileHandler.deleteRecord(TEST_REPO_ID, TEST_RECORD_ID)).thenReturn(true);
        assertThat(repositoryManager.deleteRecord(TEST_REPO_ID, TEST_RECORD_ID)).isTrue();
        
        when(carFileHandler.getRecord(TEST_REPO_ID, TEST_RECORD_ID)).thenReturn(null);
        assertThat(repositoryManager.getRecord(TEST_REPO_ID, TEST_RECORD_ID)).isNull();
    }

    @Test
    public void testRepositorySync() throws IOException {
        // Test syncing repository
        when(carFileHandler.syncRepository(TEST_REPO_ID)).thenReturn(true);
        assertThat(repositoryManager.syncRepository(TEST_REPO_ID)).isTrue();

        // Test syncing non-existent repository
        when(carFileHandler.repositoryExists("nonexistent:did")).thenReturn(false);
        assertThat(repositoryManager.syncRepository("nonexistent:did")).isFalse();

        // Test syncing with conflicts
        when(carFileHandler.hasConflicts(TEST_REPO_ID)).thenReturn(true);
        assertThat(repositoryManager.syncRepository(TEST_REPO_ID)).isFalse();
    }

    @Test
    public void testErrorHandling() {
        // Test invalid DID format
        assertThatThrownBy(() -> repositoryManager.createRepository("invalid:did"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid DID format");

        // Test non-existent repository
        when(carFileHandler.repositoryExists(TEST_REPO_ID)).thenReturn(false);
        assertThatThrownBy(() -> repositoryManager.getRecord(TEST_REPO_ID, TEST_RECORD_ID))
            .isInstanceOf(RepositoryNotFoundException.class);

        // Test record not found
        when(carFileHandler.getRecord(TEST_REPO_ID, TEST_RECORD_ID)).thenReturn(null);
        assertThat(repositoryManager.getRecord(TEST_REPO_ID, TEST_RECORD_ID)).isNull();

        // Test invalid record data
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("text", new Object()); // Invalid type
        
        assertThatThrownBy(() -> repositoryManager.addRecord(TEST_REPO_ID, TEST_RECORD_ID, invalidData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid record data");
    }
}
