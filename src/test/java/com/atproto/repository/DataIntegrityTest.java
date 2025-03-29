package com.atproto.repository;

import com.atproto.syntax.Cid;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(records).containsKey(recordPath);
        
        // And: CAR file can be read back correctly
        Repository fromCar = Repository.fromCarFile(carFile);
        assertThat(fromCar.getLatestVersion()).isEqualTo(repository.getLatestVersion());
        assertThat(fromCar.getRecords()).isEqualTo(repository.getRecords());
    }
    
    @ParameterizedTest
    @CsvSource({
        "bafkreic4j7375b4273q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q, true",
        "invalid-cid, false",
        "bafkreic4j7375b4273q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3q3qextra, false",
        "short, false"
    })
    void testCidValidation(String cidStr, boolean isValid) {
        if (isValid) {
            Cid cid = Cid.fromBase32(cidStr);
            assertThat(cid).isNotNull();
            assertThat(cid.toBase32()).isEqualTo(cidStr);
        } else {
            assertThatThrownBy(() -> Cid.fromBase32(cidStr))
                .isInstanceOf(IllegalArgumentException.class);
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
        assertThat(records).containsKeys(path1, path2, path3);
        
        // And: Data retrieval should match original data
        byte[] retrieved1 = repository.getRecord(path1);
        byte[] retrieved2 = repository.getRecord(path2);
        byte[] retrieved3 = repository.getRecord(path3);
        
        assertThat(retrieved1).isEqualTo(data1);
        assertThat(retrieved2).isEqualTo(data2);
        assertThat(retrieved3).isEqualTo(data3);
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
        assertThat(version1).isNotEqualTo(initialVersion);
        assertThat(version2).isNotEqualTo(version1);
        
        // And: Version history should be maintained
        Map<Version, Map<String, Cid>> history = repository.getVersionHistory();
        assertThat(history).containsKeys(initialVersion, version1, version2);
        
        // And: Records should exist in correct versions
        Map<String, Cid> records1 = history.get(version1);
        assertThat(records1).containsKey(path1);
        assertThat(records1).doesNotContainKey(path2);
        
        Map<String, Cid> records2 = history.get(version2);
        assertThat(records2).containsKeys(path1, path2);
    }
}
