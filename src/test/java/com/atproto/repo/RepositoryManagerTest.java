package com.atproto.repo;

import com.atproto.did.DidResolver;
import com.atproto.lexicon.LexiconParser;
import com.atproto.xrpc.XrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class RepositoryManagerTest {
    @TempDir
    Path tempDir;

    @Mock
    private XrpcClient xrpcClient;

    @Mock
    private DidResolver didResolver;

    @Mock
    private LexiconParser lexiconParser;

    @InjectMocks
    private RepositoryManager repositoryManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Configure mocks with default behaviors
        when(xrpcClient.sendRequest(any(), any())).thenReturn(new byte[0]);
        when(didResolver.resolveDid(any())).thenReturn("did:plc:example".getBytes());
    }

    @Test
    void testCARFileOperations() throws IOException {
        // Create a test CAR file
        Path carFilePath = tempDir.resolve("test.car");
        byte[] testData = new byte[1024];
        
        // Test CAR file creation
        repositoryManager.createCARFile(carFilePath, testData);
        assertThat(carFilePath).exists();
        
        // Test CAR file validation
        boolean isValid = repositoryManager.validateCARFile(carFilePath);
        assertThat(isValid).isTrue();
        
        // Test CAR file reading
        byte[] readData = repositoryManager.readCARFile(carFilePath);
        assertThat(readData).isEqualTo(testData);
    }

    @Test
    void testRepositorySynchronization() throws IOException {
        // Mock repository data
        String did = "did:plc:test";
        String repoRoot = UUID.randomUUID().toString();
        
        // Test repository creation
        repositoryManager.createRepository(did, repoRoot);
        assertThat(tempDir.resolve(repoRoot)).exists();
        
        // Test repository synchronization
        repositoryManager.syncRepository(did, repoRoot);
        
        // Verify synchronization
        assertThat(repositoryManager.isRepositorySynced(did, repoRoot)).isTrue();
    }

    @Test
    void testDataIntegrityVerification() throws IOException {
        // Create test data
        String did = "did:plc:test";
        String repoRoot = UUID.randomUUID().toString();
        byte[] originalData = new byte[1024];
        
        // Store data
        repositoryManager.storeData(did, repoRoot, "test", originalData);
        
        // Verify data integrity
        boolean isValid = repositoryManager.verifyDataIntegrity(did, repoRoot, "test");
        assertThat(isValid).isTrue();
        
        // Test with corrupted data
        byte[] corruptedData = new byte[1023];
        repositoryManager.storeData(did, repoRoot, "test", corruptedData);
        isValid = repositoryManager.verifyDataIntegrity(did, repoRoot, "test");
        assertThat(isValid).isFalse();
    }

    @Test
    void testRepositoryVersioning() throws IOException {
        // Create test repository
        String did = "did:plc:test";
        String repoRoot = UUID.randomUUID().toString();
        repositoryManager.createRepository(did, repoRoot);
        
        // Test version creation
        String version1 = repositoryManager.createVersion(did, repoRoot);
        assertThat(version1).isNotNull();
        
        // Test version retrieval
        byte[] data = new byte[1024];
        repositoryManager.storeData(did, repoRoot, "test", data);
        
        // Test version history
        String[] versions = repositoryManager.getVersionHistory(did, repoRoot);
        assertThat(versions).isNotEmpty();
    }

    @Test
    void testRepositoryCleanup() throws IOException {
        // Create test repository
        String did = "did:plc:test";
        String repoRoot = UUID.randomUUID().toString();
        repositoryManager.createRepository(did, repoRoot);
        
        // Store some data
        byte[] data = new byte[1024];
        repositoryManager.storeData(did, repoRoot, "test", data);
        
        // Test cleanup
        repositoryManager.cleanupRepository(did, repoRoot);
        assertThat(tempDir.resolve(repoRoot)).doesNotExist();
    }
}
