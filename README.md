# Java-Atproto

A comprehensive Java implementation of the ATProtocol, a decentralized social networking protocol designed by Bluesky.

## Overview

ATProtocol (ActivityPub Transport Protocol) is a decentralized social networking protocol that enables interoperable social networks without central control. This Java implementation provides a robust and efficient way to interact with ATProtocol-based services, including Bluesky.

## Key Features

- **XRPC Implementation**: Complete support for ATProtocol's HTTP-based RPC protocol
- **Lexicon Support**: Schema definition and validation using ATProtocol's Lexicon language
- **DID Integration**: Decentralized Identifier support for user authentication
- **Repository Management**: Efficient handling of CAR files and repository operations
- **Event System**: Real-time event streaming and subscription management
- **Moderation System**: Content moderation and policy enforcement
- **Performance Optimizations**: Thread-safe operations and memory management
- **Security Features**: Robust authentication and encryption support

## Project Structure

```
atproto-java/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/
│   │   │   │   ├── atproto/
│   │   │   │   │    ├── api/           # XRPC client and server implementation
│   │   │   │   │    │    ├── AtpAgent.java
│   │   │   │   │    │    ├── AtpResponse.java
│   │   │   │   │    │    ├── AtpSession.java
│   │   │   │   │    │    └── xrpc/
│   │   │   │   │    │        ├── XrpcClient.java
│   │   │   │   │    │        ├── XrpcRequest.java
│   │   │   │   │    │        ├── XrpcResponse.java
│   │   │   │   │    │        ├── XrpcException.java
│   │   │   │   │    │        ├── model/Params.java
│   │   │   │   │    │        └── HttpUtil.java
│   │   │   │   │    ├── common/        # Shared utilities and models
│   │   │   │   │    │    ├── AtProtoException.java
│   │   │   │   │    │    ├── Nsid.java
│   │   │   │   │    │    ├── Cid.java
│   │   │   │   │    │    ├── AtUri.java
│   │   │   │   │    │    ├── Did.java
│   │   │   │   │    │    └── Handle.java
│   │   │   │   │    ├── crypto/        # Cryptographic operations
│   │   │   │   │    │    ├── SigningUtil.java
│   │   │   │   │    │    └── KeySerialization.java
│   │   │   │   │    ├── identity/      # DID and authentication
│   │   │   │   │    │    ├── IdentityResolver.java
│   │   │   │   │    │    ├── DidResolver.java
│   │   │   │   │    │    └── HandleResolver.java
│   │   │   │   │    ├── models/        # Generated lexicon models
│   │   │   │   │    │    ├── com/
│   │   │   │   │    │    │    ├── atproto/
│   │   │   │   │    │    │    │    ├── repo/
│   │   │   │   │    │    │    │    │    ├── CreateRecordRequest.java  **[GENERATED]**
│   │   │   │   │    │    │    │    │    ├── CreateRecordResponse.java **[GENERATED]**
│   │   │   │   │    │    │    │    │    ├── ListRecordsRequest.java   **[GENERATED]**
│   │   │   │   │    │    │    │    │    └── ListRecordsResponse.java  **[GENERATED]**
│   │   │   │   │    │    │    │    └── identity/  **[GENERATED]**
│   │   │   │   │    │    │    └── app/
│   │   │   │   │    │    │         ├── bsky/
│   │   │   │   │    │    │         │    ├── feed/
│   │   │   │   │    │    │         │    │    ├── GetTimelineRequest.java  **[GENERATED]**
│   │   │   │   │    │    │         │    │    ├── GetTimelineResponse.java **[GENERATED]**
│   │   │   │   │    │    │         │    │    └── Post.java                **[GENERATED]**
│   │   │   │   │    │    │         └── ...                            **[GENERATED]**
│   │   │   │   │    ├── RichText.java  # Manually created, handles rich text
│   │   │   │   │    ├── moderation/    # Content moderation system
│   │   │   │   │    │    └── ModerationAction.java # Manually created
│   │   │   │   │    └── codegen/       # Lexicon code generation
│   │   │   │   │         ├── LexiconParser.java
│   │   │   │   │         ├── ModelGenerator.java
│   │   │   │   │         ├── ClientGenerator.java
│   │   │   │   │         └── Generator.java
│   ├── test/
│   │   ├── java/
│   │   │   ├── com/
│   │   │   │   ├── atproto/
│   │   │   │   │    ├── ClientTest.java
│   │   │   │   │    ├── api/          # XRPC test cases
│   │   │   │   │    │    ├── AtpAgentTest.java
│   │   │   │   │    │    └── xrpc/
│   │   │   │   │    │        └── XrpcClientTest.java
│   │   │   │   │    ├── common/       # Common utilities tests
│   │   │   │   │    │    └── AtUriTest.java
│   │   │   │   │    ├── crypto/       # Cryptographic tests
│   │   │   │   │    │    └── ...
│   │   │   │   │    ├── identity/      # DID and authentication tests
│   │   │   │   │    │    └── ...
│   │   │   │   │    ├── models/        # Model tests
│   │   │   │   │    │    ├── com/
│   │   │   │   │    │    │    ├── atproto/
│   │   │   │   │    │    │    │    ├── repo/
│   │   │   │   │    │    │    │    │    └── CreateRecordRequestTest.java
│   │   │   │   │    │    │    └── app/
│   │   │   │   │    │    └── ...
│   │   │   │   │    ├── moderation/    # Moderation system tests
│   │   │   │   │    │    └── ModerationActionTest.java
│   │   │   │   │    └── codegen/       # Code generation tests
│   │   │   │   │         ├── LexiconParserTest.java
│   │   │   │   │         ├── ModelGeneratorTest.java
│   │   │   │   │         └── ClientGeneratorTest.java
│   │   └── resources/
│   │       └── lexicons/              # Lexicon schema definitions
│   │            └── com/
│   │                 └── example/
│   │                      └── test.json
├── pom.xml
├── README.md
├── LICENSE
└── docs/                          # Javadoc and documentation
    ├── README.md
    └── chats/

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8.0 or higher
- Git

### Installation

1. Clone the repository:
```bash
git clone https://github.com/dabockster/java-atprotocol.git
cd java-atprotocol
```

2. Build the project:
```bash
mvn clean install
```

### Usage

```java
// Create a new client instance
Client client = new Client("https://bsky.social", "your-access-token");

// Make an XRPC request
Map<String, Object> result = client.sendXrpcRequest(
    "com.atproto.identity.resolveHandle",
    Map.of("handle", "test.handle")
);
```

## Testing

The project includes comprehensive test coverage across all major components:

- **Unit Tests**: Individual component testing
- **Integration Tests**: End-to-end protocol flows
- **Performance Tests**: Benchmarking critical operations
- **Security Tests**: Authentication and encryption validation
- **Lexicon Tests**: Schema parsing and validation
- **Repository Tests**: CAR file operations and sync
- **Event System Tests**: Streaming and subscription management

To run the tests:
```bash
mvn test
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## References

- [ATProtocol Specification](https://atproto.com/specs)
- [Bluesky Documentation](https://bsky.social/docs)
- [Lexicon Language](https://atprotocol.com/specs/lexicon)
- [XRPC Protocol](https://atprotocol.com/specs/xrpc)
- [TypeScript Implementation](https://github.com/bluesky-social/atproto-ts) - Official TypeScript implementation
- [Go Implementation](https://github.com/bluesky-social/indigo) - Official Go implementation