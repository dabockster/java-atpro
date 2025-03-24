# Java-Atpro

This is a Java implementation of the ATProtocol.

## Directory Structure

```
atproto-java/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/
│   │   │   │   ├── atproto/
│   │   │   │       ├── api/
│   │   │   │       │    ├── AtpAgent.java
│   │   │   │       │    ├── AtpResponse.java
│   │   │   │       │    ├── AtpSession.java
│   │   │   │       │    ├── xrpc/
│   │   │   │       │    │    ├── XrpcClient.java
│   │   │   │       │    │    ├── XrpcRequest.java
│   │   │   │       │    │    ├── XrpcResponse.java
│   │   │   │       │    │    ├── XrpcException.java
│   │   │   │       │    │    ├── model/
│   │   │   │       │    │    │   └── Params.java
│   │   │   │       │    │    └── HttpUtil.java  // Moved up a level
│   │   │   │       ├── common/
│   │   │   │       │    ├── AtProtoException.java
│   │   │   │       │    ├── Nsid.java
│   │   │   │       │    ├── Cid.java
│   │   │   │       │    ├── AtUri.java
│   │   │   │       │    ├── Did.java
│   │   │   │       │    └── Handle.java
│   │   │   │       ├── crypto/
│   │   │   │       │    ├── SigningUtil.java
│   │   │   │       │    └── KeySerialization.java
│   │   │   │       ├── identity/
│   │   │   │       │    ├── IdentityResolver.java
│   │   │   │       │    ├── DidResolver.java
│   │   │   │       │    └── HandleResolver.java
│   │   │   │       ├── models/      // All files under here are GENERATED
│   │   │   │       │    ├── com/
│   │   │   │       │    │   ├── atproto/
│   │   │   │       │    │   │   ├── repo/
│   │   │   │       │    │   │   │   ├── CreateRecordRequest.java  **[GENERATED]**
│   │   │   │       │    │   │   │   ├── CreateRecordResponse.java **[GENERATED]**
│   │   │   │       │    │   │   │   ├── ListRecordsRequest.java   **[GENERATED]**
│   │   │   │       │    │   │   │   └── ListRecordsResponse.java  **[GENERATED]**
│   │   │   │       │    │   │   ├── identity/  **[GENERATED]**
│   │   │   │       │    │   │   └── ...          **[GENERATED]**
│   │   │   │       │    │   └── ...             **[GENERATED]**
│   │   │   │       │    ├── app/
│   │   │   │       │    │   ├── bsky/
│   │   │   │       │    │   │   ├── feed/
│   │   │   │       │    │   │   │   ├── GetTimelineRequest.java  **[GENERATED]**
│   │   │   │       │    │   │   │   ├── GetTimelineResponse.java **[GENERATED]**
│   │   │   │       │    │   │   │   └── Post.java                **[GENERATED]**
│   │   │   │       │    │   │   └── ...                         **[GENERATED]**
│   │   │   │       │    │   └── ...                            **[GENERATED]**
│   │   │   │       │    └── ...                                 **[GENERATED]**
│   │   │   │       ├── RichText.java  // Manually created, handles rich text.
│   │   │   │       ├── moderation/
│   │   │   │       │    └── ModerationAction.java // Manually created
│   │   │   │       └── codegen/
│   │   │   │            ├── LexiconParser.java
│   │   │   │            ├── ModelGenerator.java
│   │   │   │            ├── ClientGenerator.java
│   │   │   │            └── Generator.java
│   ├── test/
│   │   ├── java/
│   │   │   ├── com/
│   │   │   │   ├── atproto/
│   │   │   │   │   ├── api/
│   │   │   │   │   │    ├── AtpAgentTest.java
│   │   │   │   │   │    └── xrpc/
│   │   │   │   │   │        └── XrpcClientTest.java
│   │   │   │   │   ├── common/
│   │   │   │   │   │    └── AtUriTest.java  // Example test
│   │   │   │   │   ├── crypto/
│   │   │   │   │   │   └── ...
│   │   │   │   │   ├── identity/
│   │   │   │   │   │   └── ...
│   │   │   │   │   ├── models/   //  Tests *should not* be generated. You write tests for your *manually created* code.
│   │   │   │   │   │    ├── com/
│   │   │   │   │   │    │    ├── atproto/
│   │   │   │   │   │    │    │    ├── repo/
│   │   │   │   │   │    │    │    │   ├── CreateRecordRequestTest.java   // Example test
│   │   │   │   │   │    │    │    │   └── ...
│   │   │   │   │   │    │    │    └── ...
│   │   │   │   │   │    │    └── ...
│   │   │   │   │   │    └── app/
│   │   │   │   │   │          └── ...
│   │   │   │   │   ├── moderation/
│   │   │   │   │   │    └── ModerationActionTest.java
│   │   │   │   │   └── codegen/
│   │   │   │   │        ├── LexiconParserTest.java
│   │   │   │   │        ├── ModelGeneratorTest.java
│   │   │   │   │        └── ClientGeneratorTest.java
│   │   └── resources/
│   │       └── lexicons/
│   │            └── com/
│   │                └── example/
│   │                    └── test.json
├── pom.xml
├── README.md
├── LICENSE
└── docs/           // Javadoc and other documentation
    ├── README.md
    └── chats/
```