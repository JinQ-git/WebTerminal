# WebSocket Message Format

Message format is based on [JsonRPC](https://www.jsonrpc.org/specification). But not like **JsonRPC** our message format does not contains `jsonrpc` member.

## Basic Form of Message

### Request Form

```json
{
    "id" : (number),
    "method": (string),
    "params": (any)
}
```

### Response Form

```json
// On Success Response
{
    "id": (number), // `id` from Request Form
    "result": (any)
}

// On Error Response
{
    "id": (number), // `id` from Request Form
    "error": (any) // Generally, Error String
}
```

### Notification Form

> NOTE: No response for **Notification** message.

```json
{
    "method": (string),
    "params": (any)
}
```

## Specification of each Message

### 1. Connection Request

```json
{
    "id": (number),
    "method": "connect",
    "params": ( Common Connection Param | Secured Connection Param )
}
```

#### 1-1. Common Connection Param

```json
// Not Secured
"params": {
    "isSecured": false, // (optional)
    "data": {
        "id": (string),
        "pw": (string),
        "host": (string) // (optional) hostname[:port]
    }
}
```

#### 1-2. Secured Connection Param

```json
// Secured
"params": {
    "isSecured": true,
    "data": {
        "cipherData": (string), // RSA Encrypted Cipher Text
        "encoding": (string) // Encoding method of `cipherData` ( "base64" | "hex" )
    }
}
```

`cipherData` is RSA encrypted data of following JSON string.

```json
{
    "id": (string),
    "pw": (string),
    "host": (string),    // (optional) hostname[:port]
    "key": (string),     // Secret Key of AES
    "encoding": (string) // Encoding method of `key` ( "base64" | "hex" )
}
```

### 2. Connection Response

If `result` member is specified, then request has been succeeded.

```json
{
    "id": (number), 
    "result": "Success"
}
```

If `error` member is specified, then request has been failed.

```json
{
    "id": (number),
    "error": (string) // Failed Reason
}
```

### 3. Data Notification

Both client and server send **Data Notification** message.

```json
{
    "method": "data",
    "params": ( Common Data Param | Secured Data Param )
}
```

#### 3-1. Common Data Param

```json
"params": {
    "isSecured": false, // (optional)
    "data": (string)
}
```

#### 3-2. Secured Data Param

```json
"params": {
    "isSecured": true,
    "data": {
        "iv": (string), // AES Initial Vector for `cipherData`
        "cipherData": (string), // AES encrypted data
        "encoding": (string) // Encoding method of both `iv` and `cipherData` ( "base64" | "hex" )
    }
}
```

Secret Key of AES must have been transferred on `Connection Request`.

### 4. Log Notification

Message for debug. Generally, server sends log message to client.

```json
{
    "method": "data",
    "params": {
        "type": ("info" | "warn" | "error"),
        "msg": (string)
    }
}
```