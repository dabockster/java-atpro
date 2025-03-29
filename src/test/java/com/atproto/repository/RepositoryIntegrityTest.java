package com.atproto.repository;

import com.atproto.syntax.Cid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class RepositoryIntegrityTest {
    @TempDir
    Path tempDir;
    
    private Repository repository;
    
    @BeforeEach
    void setUp() throws IOException {
        repository = new Repository(tempDir);
        repository.initialize();
    }
    
    @Test
    void testRepositoryInitialization() throws IOException {
        assertTrue(repository.isInitialized());
        assertNotNull(repository.getLatestVersion());
        assertNotNull(repository.getRootCid());
    }
    
    @Test
    void testVersionControl() throws IOException {
        // Initial state
        Version initialVersion = repository.getLatestVersion();
        
        // Add a record
        String recordPath = "com.example.record";
        byte[] recordData = "test data".getBytes();
        Cid recordCid = repository.putRecord(recordPath, recordData);
        
        // Verify version changed
        Version newVersion = repository.getLatestVersion();
        assertNotEquals(initialVersion, newVersion);
        
        // Verify record exists
        Map<String, Cid> records = repository.getRecords();
        assertTrue(records.containsKey(recordPath));
        assertEquals(recordCid, records.get(recordPath));
    }
    
    @Test
    void testRepositoryConsistency() throws IOException {
        // Add multiple records
        String path1 = "com.example.record1";
        String path2 = "com.example.record2";
        byte[] data1 = "data1".getBytes();
        byte[] data2 = "data2".getBytes();
        
        Cid cid1 = repository.putRecord(path1, data1);
        Cid cid2 = repository.putRecord(path2, data2);
        
        // Verify records are consistent
        Map<String, Cid> records = repository.getRecords();
        assertEquals(cid1, records.get(path1));
        assertEquals(cid2, records.get(path2));
        
        // Verify CAR file consistency
        CarFile carFile = repository.createCarFile();
        Repository fromCar = Repository.fromCarFile(carFile);
        assertEquals(repository.getLatestVersion(), fromCar.getLatestVersion());
        assertEquals(repository.getRecords(), fromCar.getRecords());
    }
    
    @Test
    void testRepositoryRollback() throws IOException {
        // Record initial state
        Version initialVersion = repository.getLatestVersion();
        
        // Add a record
        String recordPath = "com.example.record";
        byte[] recordData = "test data".getBytes();
        repository.putRecord(recordPath, recordData);
        
        // Rollback to initial state
        repository.rollbackToVersion(initialVersion);
        
        // Verify rollback
        assertEquals(initialVersion, repository.getLatestVersion());
        Map<String, Cid> records = repository.getRecords();
        assertFalse(records.containsKey(recordPath));
    }
}
