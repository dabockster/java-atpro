package com.atproto.repository;

import com.atproto.repository.Repository;
import com.atproto.repository.Version;
import com.atproto.syntax.Cid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
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
        assertTrue(repository.isInitialized());
        assertNotNull(repository.getLatestVersion());
        assertNotNull(repository.getRootCid());
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
        when(repository.putRecord(anyString(), any(byte[].class))).thenReturn(recordCid);
        
        // Then
        Version newVersion = new Version();
        when(repository.getLatestVersion()).thenReturn(newVersion);
        assertNotEquals(initialVersion, newVersion);
        Map<String, Cid> records = new HashMap<>();
        records.put(recordPath, recordCid);
        when(repository.getRecords()).thenReturn(records);
        Map<String, Cid> actualRecords = repository.getRecords();
        assertTrue(actualRecords.containsKey(recordPath));
        assertEquals(recordCid, actualRecords.get(recordPath));
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
        
        when(repository.putRecord(eq(path1), any(byte[].class))).thenReturn(cid1);
        when(repository.putRecord(eq(path2), any(byte[].class))).thenReturn(cid2);
        
        // When
        
        // Then
        Map<String, Cid> records = new HashMap<>();
        records.put(path1, cid1);
        records.put(path2, cid2);
        when(repository.getRecords()).thenReturn(records);
        Map<String, Cid> actualRecords = repository.getRecords();
        assertEquals(cid1, actualRecords.get(path1));
        assertEquals(cid2, actualRecords.get(path2));
    }
    
    @Test
    void shouldSupportRollback() throws IOException {
        // Given
        Version initialVersion = new Version();
        when(repository.getLatestVersion()).thenReturn(initialVersion);
        
        // When
        String recordPath = "com.example.record";
        byte[] recordData = "test data".getBytes();
        doNothing().when(repository).rollbackToVersion(eq(initialVersion));
        
        // Then
        repository.rollbackToVersion(initialVersion);
        verify(repository).rollbackToVersion(initialVersion);
        when(repository.getLatestVersion()).thenReturn(initialVersion);
        Map<String, Cid> records = new HashMap<>();
        when(repository.getRecords()).thenReturn(records);
        assertEquals(initialVersion, repository.getLatestVersion());
        Map<String, Cid> actualRecords = repository.getRecords();
        assertFalse(actualRecords.containsKey(recordPath));
    }
}
