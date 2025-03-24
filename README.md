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
│   │   │   │       ├── api/        // API-specific code (client logic, XRPC)
│   │   │   │       │    ├── AtpAgent.java
│   │   │   │       │    ├── AtpResponse.java
│   │   │   │       │    ├── AtpSession.java
│   │   │   │       │    ├── xrpc/
│   │   │   │       │    │    ├── XrpcClient.java
│   │   │   │       │    │    ├── XrpcRequest.java
│   │   │   │       │    │    ├── XrpcResponse.java
│   │   │   │       │    │    ├── XrpcException.java
│   │   │   │       │    │    ├── models/
│   │   │   │       │    │    │    └── Params.java
│   │   │   │       │    └── ...
│   │   │   │       ├── common/      // Shared code and utilities
│   │   │   │       │    ├── AtProtoException.java
│   │   │   │       │    └── ...
│   │   │   │       ├── crypto/      // Cryptographic utilities (simplified)
│   │   │   │       │    ├── SigningUtil.java
│   │   │   │       │    └── KeySerialization.java
│   │   │   │       ├── identity/    // DID and handle resolution
│   │   │   │       │    ├── IdentityResolver.java
│   │   │   │       │    └── ...
│   │   │   │       ├── models/      // Data models (mirroring Lexicon namespaces)
│   │   │   │       │    ├── com/
│   │   │   │       │    │   ├── atproto/
│   │   │   │       │    │   │   ├── repo/
│   │   │   │       │    │   │   │   ├── CreateRecordRequest.java
│   │   │   │       │    │   │   │   └── ...
│   │   │   │       │    │   │   └── ...
│   │   │   │       │    │   └── ...
│   │   │   │       │    ├── app/
│   │   │   │       │    │   ├── bsky/
│   │   │   │       │    │   │    ├── feed/
│   │   │   │       │    │   │    │    └── Post.java
│   │   │   │       │    │   │    └── ...
│   │   │   │       │    │   └── ...
│   │   │   │        │    └── ...
│   │   │   │       ├── RichText.java    // RichText processing
│   │   │    	   ├── moderation/
│   │   │   │	   │         └── ModerationAction.java
│   │   │   │       └── typeguards/
│   │   │   │            ├── AppBskyFeedDefs.java
│   │   │   │            ├── AppBskyFeedPost.java
│   │   │   │            └── ...
│   │   ├── test/
│   │   │   ├── java/
│   │   │   │  ├── com/
│   │   │   │  │   ├── atproto/
│   │   │   │  │       ├── api/
│   │   │   │  │       │   ├── AtpAgentTest.java
│   │   │   │  │       │   ├── xrpc/
│   │   │   │  │       │   │   ├── models/
│   │   │   │  │       │   │   │   └── ParamsTest.java
│   │   │   │  │       │   │   └── XrpcClientTest.java
│   │   │   │  │       ├── common/
│   │   │   │  │       ├── crypto/
│   │   │   │  │       ├── identity/
│   │   │   │  │       ├── models/
│   │   │   │  │       │    ├── com/
│   │   │   │  │       │    │   └── ...  // Tests for model classes
│   │   │   │  │       │    └── ...
│   │   │   │	     ├── moderation/ (Moderation related functionality)
│   │   │   │       │	     ├── ModerationActionTest.java	
│   │   │   │       └── ... // Other test classes
│   └── resources/  // (Test resources, if any)
│
├── build.gradle.kts (or pom.xml)
├── README.md
└── LICENSE
```