# FPMBuild AI Agent Protocol

Start `fpmbuild agent --stdio` and communicate with newline-delimited JSON.
Stdout is reserved for responses. Diagnostics and child-build output go to stderr.

## Request

```json
{"id":"request-1","command":"inspect-project","params":{"path":"/workspace/mod"}}
```

`id` is opaque and is echoed back. `params` is converted to CLI options.
Arrays become repeated options. `args` may be used instead when exact CLI control is needed.

## Response

Success:

```json
{"id":"request-1","ok":true,"result":{}}
```

Failure:

```json
{"id":"request-1","ok":false,"error":"IllegalArgumentException","message":"..."}
```

## Recommended workflow for an agent

1. `capabilities`
2. `inspect-project`
3. `build-project` if no current primary artifact exists
4. `bytecode-context` before any future transformation
5. `build` when producing an FPM package from a spec

The future bytecode API should use the SHA-256 returned by `bytecode-context` as an
optimistic-concurrency guard before replacing a project JAR.
