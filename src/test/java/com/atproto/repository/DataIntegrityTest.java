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
        // Given: A repository with test records
        String recordPath = "com.example.record";
        byte[] recordData = "test data".getBytes();
        repository.putRecord(recordPath, recordData);
        
        // When: Creating a CAR file
        CarFile carFile = repository.createCarFile();
        
        // Then: CAR file contains all records
        Map<String, Cid> records = carFile.getRecords();
        assertTrue(records.containsKey(recordPath), "CAR file should contain the record");
        
        // And: CAR file can be read back correctly
        Repository fromCar = Repository.fromCarFile(carFile);
        assertEquals(repository.getLatestVersion(), fromCar.getLatestVersion(), "Versions should match");
        assertEquals(repository.getRecords(), fromCar.getRecords(), "Records should match");
    }
    
    @Test
    void testCidValidation() {
        // Given: Valid and invalid CID strings
        String[] validCids = {
            "bafkreic4j7375b4273q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q",
            "bafkreic4j7375b4273q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q"
        };
        
        // When: Validating valid CIDs
        for (String cidStr : validCids) {
            Cid cid = Cid.fromBase32(cidStr);
            assertNotNull(cid, "CID should not be null");
            assertEquals(cidStr, cid.toBase32(), "CID round trip should match original");
        }
        
        // Then: Invalid CIDs should throw IllegalArgumentException
        String[] invalidCids = {
            "invalid-cid",
            "bafkreic4j7375b4273q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3qextra",
            "short"
        };
        
        for (String cidStr : invalidCids) {
            assertThrows(IllegalArgumentException.class, 
                () -> Cid.fromBase32(cidStr),
                "Invalid CID should throw IllegalArgumentException"
            );
        }
    }
    
    @Test
    void testDataConsistency() throws IOException {
        // Given: A repository with multiple records
        String path1 = "com.example.record1";
        String path2 = "com.example.record2";
        String path3 = "com.example.record3";
        
        // When: Adding different types of data
        byte[] data1 = "Hello World".getBytes();
        repository.putRecord(path1, data1);
        
        String json = "{\"name\":\"test\",\"value\":42}";
        byte[] data2 = json.getBytes();
        repository.putRecord(path2, data2);
        
        byte[] data3 = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        repository.putRecord(path3, data3);
        
        // Then: All records should be present and correct
        Map<String, Cid> records = repository.getRecords();
        assertTrue(records.containsKey(path1), "Record 1 should be present");
        assertTrue(records.containsKey(path2), "Record 2 should be present");
        assertTrue(records.containsKey(path3), "Record 3 should be present");
        
        // And: Data retrieval should match original data
        byte[] retrieved1 = repository.getRecord(path1);
        byte[] retrieved2 = repository.getRecord(path2);
        byte[] retrieved3 = repository.getRecord(path3);
        
        assertArrayEquals(data1, retrieved1, "Record 1 data should match");
        assertArrayEquals(data2, retrieved2, "Record 2 data should match");
        assertArrayEquals(data3, retrieved3, "Record 3 data should match");
    }
    
    @Test
    void testVersionConsistency() throws IOException {
        // Given: A repository with initial state
        Version initialVersion = repository.getLatestVersion();
        
        // When: Adding multiple records
        String path1 = "com.example.record1";
        String path2 = "com.example.record2";
        byte[] data1 = "data1".getBytes();
        byte[] data2 = "data2".getBytes();
        
        repository.putRecord(path1, data1);
        Version version1 = repository.getLatestVersion();
        
        repository.putRecord(path2, data2);
        Version version2 = repository.getLatestVersion();
        
        // Then: Versions should progress correctly
        assertNotEquals(initialVersion, version1, "Version 1 should be different from initial");
        assertNotEquals(version1, version2, "Version 2 should be different from version 1");
        
        // And: Version history should be maintained
        Map<Version, Map<String, Cid>> history = repository.getVersionHistory();
        assertTrue(history.containsKey(initialVersion), "Initial version should be in history");
        assertTrue(history.containsKey(version1), "Version 1 should be in history");
        assertTrue(history.containsKey(version2), "Version 2 should be in history");
        
        // And: Records should exist in correct versions
        Map<String, Cid> records1 = history.get(version1);
        assertTrue(records1.containsKey(path1), "Record 1 should exist in version 1");
        assertFalse(records1.containsKey(path2), "Record 2 should not exist in version 1");
        
        Map<String, Cid> records2 = history.get(version2);
        assertTrue(records2.containsKey(path1), "Record 1 should exist in version 2");
        assertTrue(records2.containsKey(path2), "Record 2 should exist in version 2");
    }
}
