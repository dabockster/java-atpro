package com.atproto.moderation;

import com.atproto.api.xrpc.XrpcRequest;
import com.atproto.api.xrpc.XrpcResponse;
import com.atproto.lexicon.LexiconLabel;
import com.atproto.lexicon.LexiconLabelValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationBehaviorTest {

    @Mock
    private XrpcRequest request;

    @InjectMocks
    private ModerationService moderationService;

    private static final String TEST_DID = "did:web:alice.test";
    private static final String TEST_HANDLE = "alice.test";
    private static final String TEST_LABEL = "porn";
    private static final String TEST_LABELER = "did:web:labeler.test";

    @BeforeEach
    void setUp() {
        // Setup mock request
        when(request.getPath()).thenReturn("/xrpc/com.atproto.moderation.label");
        when(request.getBody()).thenReturn(Map.of(
            "label", TEST_LABEL,
            "labeler", TEST_LABELER,
            "subject", Map.of(
                "did", TEST_DID
            )
        ));
    }

    @Test
    void testHideLabelBehavior() {
        ModerationResult result = moderationService.applyModeration(
            TEST_DID,
            List.of(createLabel(TEST_LABEL, TEST_LABELER, "hide"))
        );

        assertThat(result).isNotNull()
            .extracting(ModerationResult::isModerated)
            .isTrue();
        assertThat(result.getActions()).contains(ModerationAction.HIDE);
    }

    @Test
    void testIgnoreLabelBehavior() {
        ModerationResult result = moderationService.applyModeration(
            TEST_DID,
            List.of(createLabel(TEST_LABEL, TEST_LABELER, "ignore"))
        );

        assertThat(result).isNotNull()
            .extracting(ModerationResult::isModerated)
            .isFalse();
        assertThat(result.getActions()).isEmpty();
    }

    @Test
    void testMultipleLabelPrioritization() {
        List<LexiconLabel> labels = List.of(
            createLabel("porn", TEST_LABELER, "hide"),
            createLabel("adult", TEST_LABELER, "ignore")
        );

        ModerationResult result = moderationService.applyModeration(TEST_DID, labels);

        assertThat(result).isNotNull()
            .extracting(ModerationResult::isModerated)
            .isTrue();
        assertThat(result.getActions()).contains(ModerationAction.HIDE);
    }

    @Test
    void testLabelerSubscription() {
        List<LexiconLabel> labels = List.of(
            createLabel(TEST_LABEL, "did:web:unsubscribed.test", "hide"),
            createLabel(TEST_LABEL, TEST_LABELER, "ignore")
        );

        ModerationResult result = moderationService.applyModeration(TEST_DID, labels);

        assertThat(result).isNotNull()
            .extracting(ModerationResult::isModerated)
            .isFalse();
        assertThat(result.getActions()).isEmpty();
    }

    @Test
    void testAdultContentPreference() {
        ModerationResult result = moderationService.applyModeration(
            TEST_DID,
            List.of(createLabel(TEST_LABEL, TEST_LABELER, "hide")),
            false  // adultContentEnabled = false
        );

        assertThat(result).isNotNull()
            .extracting(ModerationResult::isModerated)
            .isTrue();
        assertThat(result.getActions()).contains(ModerationAction.HIDE);
    }

    private LexiconLabel createLabel(String label, String labeler, String behavior) {
        return LexiconLabel.builder()
            .src(labeler)
            .uri("at://" + labeler + "/app.bsky.actor.profile/self")
            .val(label)
            .cts(Instant.now().toString())
            .build();
    }
}
