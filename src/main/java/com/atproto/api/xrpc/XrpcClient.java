package com.atproto.api.xrpc;

import com.atproto.api.xrpc.model.XrpcRequest;
import com.atproto.api.xrpc.model.XrpcResponse;

public class XrpcClient implements XrpcClientInterface {
    @Override
    public XrpcResponse send(XrpcRequest request) throws XrpcException {
        // TO DO: implement the send method
        return null;
    }
}

interface XrpcClientInterface {
    XrpcResponse send(XrpcRequest request) throws XrpcException;
}
