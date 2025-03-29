package com.atproto.security;

import com.atproto.api.xrpc.XrpcRequest;
import com.atproto.api.xrpc.XrpcResponse;
import com.atproto.did.DID;
import com.atproto.repo.RepoRef;
import com.atproto.repo.StrongRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountTakedownTest {

    @Mock
    private XrpcRequest request;

    @InjectMocks
    private AccountModerationService moderationService;

    private static final String TEST_DID = "did:web:bob.test";
    private static final String TEST_TAKEDOWN_REF = "test-repo";
    private static final String TEST_RECORD_URI = "at://did:web:carol.test/app.bsky.feed.post/123";
    private static final String TEST_BLOB_CID = "bafkreifx234567890abcdef1234567890abcdef1234567890abcdef123456";

    @BeforeEach
    void setUp() {
        // Setup mock request
        when(request.getPath()).thenReturn("/xrpc/com.atproto.admin.updateSubjectStatus");
        when(request.getBody()).thenReturn(Map.of(
            "subject", Map.of(
                "$type", "com.atproto.admin.defs#repoRef",
                "did", TEST_DID
            ),
            "takedown", Map.of(
                "applied", true,
                "ref", TEST_TAKEDOWN_REF
            )
        ));
    }

    @Test
    void testAccountTakedown() {
        boolean result = moderationService.updateSubjectStatus(request);

        assertTrue(result);
        assertTrue(moderationService.isTakedownApplied(TEST_DID));
        assertEquals(TEST_TAKEDOWN_REF, moderationService.getTakedownRef(TEST_DID));
    }

    @Test
    void testAccountRestoration() {
        // First take down the account
        moderationService.updateSubjectStatus(request);

        // Restore the account
        when(request.getBody()).thenReturn(Map.of(
            "subject", Map.of(
                "$type", "com.atproto.admin.defs#repoRef",
                "did", TEST_DID
            ),
            "takedown", Map.of(
                "applied", false
            )
        ));

        boolean result = moderationService.updateSubjectStatus(request);

        assertTrue(result);
        assertFalse(moderationService.isTakedownApplied(TEST_DID));
        assertNull(moderationService.getTakedownRef(TEST_DID));
    }

    @Test
    void testRecordTakedown() {
        StrongRef recordRef = StrongRef.builder()
            .uri(TEST_RECORD_URI)
            .cid(TEST_BLOB_CID)
            .build();

        boolean result = moderationService.takeDownRecord(recordRef);

        assertTrue(result);
        assertTrue(moderationService.isRecordTakedownApplied(recordRef));
    }

    @Test
    void testBlobTakedown() {
        RepoRef blobRef = RepoRef.builder()
            .did(TEST_DID)
            .cid(TEST_BLOB_CID)
            .build();

        boolean result = moderationService.takeDownBlob(blobRef);

        assertTrue(result);
        assertTrue(moderationService.isBlobTakedownApplied(blobRef));
    }

    @Test
    void testTakedownStatus() {
        // Take down the account
        moderationService.updateSubjectStatus(request);

        AccountTakedownStatus status = moderationService.getSubjectStatus(TEST_DID);

        assertNotNull(status);
        assertEquals(TEST_DID, status.getDid());
        assertTrue(status.isTakedownApplied());
        assertEquals(TEST_TAKEDOWN_REF, status.getTakedownRef());
    }
}
