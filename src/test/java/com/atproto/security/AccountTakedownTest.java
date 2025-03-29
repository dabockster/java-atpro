package com.atproto.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountTakedownTest {

    @Mock
    private Object request;

    @InjectMocks
    private AccountModerationService moderationService;

    private static final String TEST_DID = "did:web:bob.test";
    private static final String TEST_TAKEDOWN_REF = "test-repo";
    private static final String TEST_RECORD_URI = "at://did:web:carol.test/app.bsky.feed.post/123";
    private static final String TEST_BLOB_CID = "bafkreifx234567890abcdef1234567890abcdef1234567890abcdef123456";

    @BeforeEach
    void setUp() {
        // Setup mock request
        when(((XrpcRequest) request).getPath()).thenReturn("/xrpc/com.atproto.admin.updateSubjectStatus");
        when(((XrpcRequest) request).getBody()).thenReturn(Map.of(
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
        boolean result = moderationService.updateSubjectStatus((XrpcRequest) request);

        assertThat(result).isTrue();
        assertThat(moderationService.isTakedownApplied(TEST_DID)).isTrue();
        assertThat(moderationService.getTakedownRef(TEST_DID)).isEqualTo(TEST_TAKEDOWN_REF);
    }

    @Test
    void testAccountRestoration() {
        // First take down the account
        moderationService.updateSubjectStatus((XrpcRequest) request);

        // Restore the account
        when(((XrpcRequest) request).getBody()).thenReturn(Map.of(
            "subject", Map.of(
                "$type", "com.atproto.admin.defs#repoRef",
                "did", TEST_DID
            ),
            "takedown", Map.of(
                "applied", false
            )
        ));

        boolean result = moderationService.updateSubjectStatus((XrpcRequest) request);

        assertThat(result).isTrue();
        assertThat(moderationService.isTakedownApplied(TEST_DID)).isFalse();
        assertThat(moderationService.getTakedownRef(TEST_DID)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"record1", "record2", "record3"})
    void testRecordTakedown(String recordRef) {
        Object record = new Object();

        boolean result = moderationService.takeDownRecord(record);

        assertThat(result).isTrue();
        assertThat(moderationService.isRecordTakedownApplied(record)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"blob1", "blob2", "blob3"})
    void testBlobTakedown(String blobRef) {
        Object blob = new Object();

        boolean result = moderationService.takeDownBlob(blob);

        assertThat(result).isTrue();
        assertThat(moderationService.isBlobTakedownApplied(blob)).isTrue();
    }

    @Test
    void testTakedownStatus() {
        // Take down the account
        moderationService.updateSubjectStatus((XrpcRequest) request);

        // Verify account status
        assertThat(moderationService.isTakedownApplied(TEST_DID)).isTrue();
        assertThat(moderationService.getTakedownRef(TEST_DID)).isEqualTo(TEST_TAKEDOWN_REF);

        // Verify record status
        Object record = new Object();
        moderationService.takeDownRecord(record);
        assertThat(moderationService.isRecordTakedownApplied(record)).isTrue();

        // Verify blob status
        Object blob = new Object();
        moderationService.takeDownBlob(blob);
        assertThat(moderationService.isBlobTakedownApplied(blob)).isTrue();
    }

    @Test
    void testMultipleTakedowns() {
        // Take down account
        moderationService.updateSubjectStatus((XrpcRequest) request);

        // Take down record
        Object record = new Object();
        moderationService.takeDownRecord(record);

        // Take down blob
        Object blob = new Object();
        moderationService.takeDownBlob(blob);

        // Verify all takedowns
        assertThat(moderationService.isTakedownApplied(TEST_DID)).isTrue();
        assertThat(moderationService.isRecordTakedownApplied(record)).isTrue();
        assertThat(moderationService.isBlobTakedownApplied(blob)).isTrue();
    }
}
