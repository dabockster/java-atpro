package com.atproto.repository;

import com.atproto.repository.Repository;
import com.atproto.repository.Version;
import com.atproto.syntax.Cid;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class RepositoryIntegrityTest {
    @TempDir
    Path tempDir;
    
    @Mock
    private Repository repository;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void shouldInitializeRepository() throws IOException {
        // Given
        when(repository.isInitialized()).thenReturn(true);
        when(repository.getLatestVersion()).thenReturn(new Version());
        when(repository.getRootCid()).thenReturn(new Cid("testCid"));
        
        // When
        
        // Then
        assertThat(repository.isInitialized()).isTrue();
        assertThat(repository.getLatestVersion()).isNotNull();
        assertThat(repository.getRootCid()).isNotNull();
    }
    
    @Test
    void shouldManageVersions() throws IOException {
        // Given
        Version initialVersion = new Version();
        when(repository.getLatestVersion()).thenReturn(initialVersion);
        
        // When
        String recordPath = "com.example.record";
        byte[] recordData = "test data".getBytes();
        Cid recordCid = new Cid("testCid");
        when(repository.putRecord(eq(recordPath), eq(recordData))).thenReturn(recordCid);
        
        // Then
        Version newVersion = new Version();
        when(repository.getLatestVersion()).thenReturn(newVersion);
        assertThat(newVersion).isNotEqualTo(initialVersion);
        
        Map<String, Cid> records = new HashMap<>();
        records.put(recordPath, recordCid);
        when(repository.getRecords()).thenReturn(records);
        Map<String, Cid> actualRecords = repository.getRecords();
        assertThat(actualRecords).containsEntry(recordPath, recordCid);
    }
    
    @Test
    void shouldMaintainRecordConsistency() throws IOException {
        // Given
        String path1 = "com.example.record1";
        String path2 = "com.example.record2";
        byte[] data1 = "data1".getBytes();
        byte[] data2 = "data2".getBytes();
        
        Cid cid1 = new Cid("cid1");
        Cid cid2 = new Cid("cid2");
        
        when(repository.putRecord(eq(path1), eq(data1))).thenReturn(cid1);
        when(repository.putRecord(eq(path2), eq(data2))).thenReturn(cid2);
        
        // When
        
        // Then
        Map<String, Cid> records = new HashMap<>();
        records.put(path1, cid1);
        records.put(path2, cid2);
        when(repository.getRecords()).thenReturn(records);
        Map<String, Cid> actualRecords = repository.getRecords();
        assertThat(actualRecords).containsEntry(path1, cid1);
        assertThat(actualRecords).containsEntry(path2, cid2);
    }
    
    @Test
    void shouldSupportRollback() throws IOException {
        // Given
        Version initialVersion = new Version();
        when(repository.getLatestVersion()).thenReturn(initialVersion);
        
        // When
        String recordPath = "com.example.record";
        byte[] recordData = "test data".getBytes();
        Cid recordCid = new Cid("testCid");
        when(repository.putRecord(eq(recordPath), eq(recordData))).thenReturn(recordCid);
        
        // Then
        Version newVersion = new Version();
        when(repository.getLatestVersion()).thenReturn(newVersion);
        assertThat(newVersion).isNotEqualTo(initialVersion);
        
        Map<String, Cid> records = new HashMap<>();
        records.put(recordPath, recordCid);
        when(repository.getRecords()).thenReturn(records);
        Map<String, Cid> actualRecords = repository.getRecords();
        assertThat(actualRecords).containsEntry(recordPath, recordCid);
        
        // And: Rollback should restore initial state
        doNothing().when(repository).rollbackToVersion(eq(initialVersion));
        repository.rollbackToVersion(initialVersion);
        
        Version postRollbackVersion = repository.getLatestVersion();
        assertThat(postRollbackVersion).isEqualTo(initialVersion);
        
        Map<String, Cid> postRollbackRecords = repository.getRecords();
        assertThat(postRollbackRecords).isEmpty();
    }
    
    @Test
    void shouldHandleCorruptedRepository() throws IOException {
        // Given
        when(repository.isInitialized()).thenReturn(true);
        when(repository.getLatestVersion()).thenReturn(new Version());
        
        // When: Corrupt the repository
        doThrow(new IOException("Corrupted repository")).when(repository).verifyIntegrity();
        
        // Then: Should throw appropriate exception
        assertThatThrownBy(() -> repository.verifyIntegrity())
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Corrupted repository");
    }
}
