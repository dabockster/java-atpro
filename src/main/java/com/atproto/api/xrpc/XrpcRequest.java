package com.atproto.api.xrpc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class XrpcRequest {
    private final String method;
    private final Map<String, Object> params;
    private final String auth;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final Map<String, Object> body;

    public XrpcRequest(String method, Map<String, Object> params, String auth, Map<String, String> headers, Map<String, String> queryParams, Map<String, Object> body) {
        this.method = method;
        this.params = params != null ? new HashMap<>(params) : new HashMap<>();
        this.auth = auth;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.queryParams = queryParams != null ? new HashMap<>(queryParams) : new HashMap<>();
        this.body = body != null ? new HashMap<>(body) : new HashMap<>();
    }

    public String getMethod() {
        return method;
    }

    public Map<String, Object> getParams() {
        return new HashMap<>(params);
    }

    public String getAuth() {
        return auth;
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    public Map<String, String> getQueryParams() {
        return new HashMap<>(queryParams);
    }

    public Map<String, Object> getBody() {
        return new HashMap<>(body);
    }

    @Override
    public String toString() {
        return "XrpcRequest{" +
                "method='" + method + '\'' +
                ", params=" + params +
                ", auth='" + auth + '\'' +
                ", headers=" + headers +
                ", queryParams=" + queryParams +
                ", body=" + body +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XrpcRequest that = (XrpcRequest) o;
        return Objects.equals(method, that.method) &&
                Objects.equals(params, that.params) &&
                Objects.equals(auth, that.auth) &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(queryParams, that.queryParams) &&
                Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, params, auth, headers, queryParams, body);
    }

    public String serialize() throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{");

        appendField(json, "method", method);
        appendField(json, "params", params);
        appendField(json, "auth", auth);
        appendField(json, "headers", headers);
        appendField(json, "queryParams", queryParams);
        appendField(json, "body", body);

        json.append("}");
        return json.toString();
    }

    private void appendField(StringBuilder json, String fieldName, Object value) {
        if (value != null) {
            json.append(json.length() > 1 ? "," : "");
            json.append('"').append(fieldName).append('"').append(":");
            appendValue(json, value);
        }
    }

    private void appendValue(StringBuilder json, Object value) {
        if (value instanceof String str) {
            json.append('"').append(str).append('"');
        } else if (value instanceof Map<?, ?> map) {
            json.append('{');
            map.forEach((k, v) -> {
                if (v != null) {
                    if (json.charAt(json.length() - 1) != '{') {
                        json.append(',');
                    }
                    appendValue(json, String.valueOf(k));
                    json.append(':');
                    appendValue(json, v);
                }
            });
            json.append('}');
        } else {
            json.append(String.valueOf(value));
        }
    }

    public static XrpcRequest deserialize(String json) throws IOException {
        // Simple JSON parser - this is a basic implementation for demonstration
        // In a real application, you might want to use a more robust JSON parser
        Map<String, Object> fields = new HashMap<>();
        
        // Remove outer braces
        String content = json.substring(1, json.length() - 1).trim();
        
        // Split fields
        String[] fieldPairs = content.split(",\\s*");
        for (String pair : fieldPairs) {
            String[] keyValue = pair.split(":\\s*");
            if (keyValue.length == 2) {
                String key = keyValue[0].replaceAll("^\\s*\"|\"\\s*$", "");
                String value = keyValue[1].trim();
                
                // Handle different value types
                if (value.startsWith("{")) {
                    fields.put(key, deserializeMap(value));
                } else if (value.startsWith("[")) {
                    fields.put(key, deserializeArray(value));
                } else if (value.startsWith("\"")) {
                    fields.put(key, value.replaceAll("^\\s*\"|\"\\s*$", ""));
                } else {
                    try {
                        fields.put(key, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        fields.put(key, Boolean.parseBoolean(value));
                    }
                }
            }
        }

        return new XrpcRequest(
            (String) fields.get("method"),
            (Map<String, Object>) fields.get("params"),
            (String) fields.get("auth"),
            (Map<String, String>) fields.get("headers"),
            (Map<String, String>) fields.get("queryParams"),
            (Map<String, Object>) fields.get("body")
        );
    }

    private static Map<String, Object> deserializeMap(String json) {
        Map<String, Object> map = new HashMap<>();
        // Remove outer braces
        String content = json.substring(1, json.length() - 1).trim();
        
        String[] fieldPairs = content.split(",\\s*");
        for (String pair : fieldPairs) {
            String[] keyValue = pair.split(":\\s*");
            if (keyValue.length == 2) {
                String key = keyValue[0].replaceAll("^\\s*\"|\"\\s*$", "");
                String value = keyValue[1].trim();
                
                if (value.startsWith("{")) {
                    map.put(key, deserializeMap(value));
                } else if (value.startsWith("[")) {
                    map.put(key, deserializeArray(value));
                } else if (value.startsWith("\"")) {
                    map.put(key, value.replaceAll("^\\s*\"|\"\\s*$", ""));
                } else {
                    try {
                        map.put(key, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        map.put(key, Boolean.parseBoolean(value));
                    }
                }
            }
        }
        return map;
    }

    private static Object[] deserializeArray(String json) {
        // Remove outer brackets
        String content = json.substring(1, json.length() - 1).trim();
        String[] values = content.split(",\\s*");
        Object[] array = new Object[values.length];
        
        for (int i = 0; i < values.length; i++) {
            String value = values[i].trim();
            
            if (value.startsWith("{")) {
                array[i] = deserializeMap(value);
            } else if (value.startsWith("[")) {
                array[i] = deserializeArray(value);
            } else if (value.startsWith("\"")) {
                array[i] = value.replaceAll("^\\s*\"|\"\\s*$", "");
            } else {
                try {
                    array[i] = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    array[i] = Boolean.parseBoolean(value);
                }
            }
        }
        return array;
    }

    public Map<String, String> validate() {
        Map<String, String> errors = new HashMap<>();

        if (method == null || method.isEmpty()) {
            errors.put("method", "Method is required");
        }

        if (method != null && !method.matches("^[a-zA-Z0-9._-]+$")) {
            errors.put("method", "Method contains invalid characters");
        }

        if (auth != null && !auth.startsWith("did:")) {
            errors.put("auth", "Auth must be a valid DID");
        }

        return errors;
    }

    public static class Builder {
        private String method;
        private Map<String, Object> params = new HashMap<>();
        private String auth;
        private Map<String, String> headers = new HashMap<>();
        private Map<String, String> queryParams = new HashMap<>();
        private Map<String, Object> body = new HashMap<>();

        public Builder withMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder withParams(Map<String, Object> params) {
            if (params != null) {
                this.params.putAll(params);
            }
            return this;
        }

        public Builder withAuth(String auth) {
            this.auth = auth;
            return this;
        }

        public Builder withHeader(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public Builder withQueryParam(String key, String value) {
            queryParams.put(key, value);
            return this;
        }

        public Builder withBody(Map<String, Object> body) {
            if (body != null) {
                this.body.putAll(body);
            }
            return this;
        }

        public XrpcRequest build() {
            return new XrpcRequest(method, params, auth, headers, queryParams, body);
        }
    }
}
