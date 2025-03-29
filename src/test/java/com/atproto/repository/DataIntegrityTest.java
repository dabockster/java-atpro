package com.atproto.repository;

import com.atproto.syntax.Cid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class DataIntegrityTest {
    @TempDir
    Path tempDir;
    
    private Repository repository;
    
    @BeforeEach
    void setUp() throws IOException {
        repository = new Repository(tempDir);
        repository.initialize();
    }
    
    @Test
    void testCarFileIntegrity() throws IOException {
        // Add some test records
        String recordPath = "com.example.record";
        byte[] recordData = "test data".getBytes();
        repository.putRecord(recordPath, recordData);
        
        // Create CAR file
        CarFile carFile = repository.createCarFile();
        
        // Verify CAR file contains all records
        Map<String, Cid> records = carFile.getRecords();
        assertTrue(records.containsKey(recordPath));
        
        // Verify CAR file can be read back correctly
        Repository fromCar = Repository.fromCarFile(carFile);
        assertEquals(repository.getLatestVersion(), fromCar.getLatestVersion());
        assertEquals(repository.getRecords(), fromCar.getRecords());
    }
    
    @Test
    void testCidValidation() {
        // Test valid CIDs
        String[] validCids = {
            "bafkreic4j7375b4273q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q",
            "bafkreic4j7375b4273q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q"
        };
        
        for (String cidStr : validCids) {
            Cid cid = Cid.fromBase32(cidStr);
            assertNotNull(cid);
            assertEquals(cidStr, cid.toBase32());
        }
        
        // Test invalid CIDs
        String[] invalidCids = {
            "invalid-cid",
            "bafkreic4j7375b4273q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3qextra",
            "short"
        };
        
        for (String cidStr : invalidCids) {
            assertThrows(IllegalArgumentException.class, () -> Cid.fromBase32(cidStr));
        }
    }
    
    @Test
    void testDataConsistency() throws IOException {
        // Add multiple records with different data types
        String path1 = "com.example.record1";
        String path2 = "com.example.record2";
        String path3 = "com.example.record3";
        
        // String data
        byte[] data1 = "Hello World".getBytes();
        repository.putRecord(path1, data1);
        
        // JSON data
        String json = "{\"name\":\"test\",\"value\":42}";
        byte[] data2 = json.getBytes();
        repository.putRecord(path2, data2);
        
        // Binary data
        byte[] data3 = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        repository.putRecord(path3, data3);
        
        // Verify all records are present and correct
        Map<String, Cid> records = repository.getRecords();
        assertTrue(records.containsKey(path1));
        assertTrue(records.containsKey(path2));
        assertTrue(records.containsKey(path3));
        
        // Verify data retrieval
        byte[] retrieved1 = repository.getRecord(path1);
        byte[] retrieved2 = repository.getRecord(path2);
        byte[] retrieved3 = repository.getRecord(path3);
        
        assertArrayEquals(data1, retrieved1);
        assertArrayEquals(data2, retrieved2);
        assertArrayEquals(data3, retrieved3);
    }
    
    @Test
    void testVersionConsistency() throws IOException {
        // Record initial state
        Version initialVersion = repository.getLatestVersion();
        
        // Add multiple records
        String path1 = "com.example.record1";
        String path2 = "com.example.record2";
        byte[] data1 = "data1".getBytes();
        byte[] data2 = "data2".getBytes();
        
        repository.putRecord(path1, data1);
        Version version1 = repository.getLatestVersion();
        
        repository.putRecord(path2, data2);
        Version version2 = repository.getLatestVersion();
        
        // Verify version progression
        assertNotEquals(initialVersion, version1);
        assertNotEquals(version1, version2);
        
        // Verify version history
        Map<Version, Map<String, Cid>> history = repository.getVersionHistory();
        assertTrue(history.containsKey(initialVersion));
        assertTrue(history.containsKey(version1));
        assertTrue(history.containsKey(version2));
        
        // Verify record existence in versions
        Map<String, Cid> records1 = history.get(version1);
        assertTrue(records1.containsKey(path1));
        assertFalse(records1.containsKey(path2));
        
        Map<String, Cid> records2 = history.get(version2);
        assertTrue(records2.containsKey(path1));
        assertTrue(records2.containsKey(path2));
    }
}
