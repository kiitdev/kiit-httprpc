# Changelog

All notable changes to kiit-rpc are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/), versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Extracted from the Kiit monorepo as its own standalone module (originally kiit-httprpc).
- `RpcRequest`/`RpcResponse`/`RpcSettings`/`RpcOptions`/`RpcClient` redesign: `RpcClient`
  collapses to one abstract `execute()` method, with `get`/`query`/`create`/`update`/`patch`/
  `delete` as defaults built on top of it. `ExecuteParams` and the local `HttpMethod` enum are
  gone, replaced by the shared `kiit.call.Verb`.
- Depends on `kiit-call` (`Identity`/`Source`/`Verb`/`Version`/`Trace`/`Content`) and `kiit-inputs`
  (`Inputs`/`Meta`), so `RpcRequest.meta`/`args` and `RpcResponse.meta`/`data` use the same shared
  types kiit-requests uses on the inbound side.
- `RpcResponse.data` is a `kiit.call.Content`, not a plain `String` — binary responses come back
  as `ContentFile`/`ContentData` with their bytes intact instead of being forced through text
  decoding.
- `RpcResponse.meta` preserves every value for a repeated header (`Set-Cookie`), not just the
  first.
- 4-step status resolution: an `x-server-status-rfc9457` response header, then an optional
  caller-supplied `StatusConverter`, then optional body parsing (`RpcSettings.parseStatusFromBody`,
  off by default), then the raw HTTP status code as a last resort.
- A failed call's `Err.ref` carries the original `RpcResponse`, so a caller isn't limited to just
  the resolved `Status` on failure.
- `RpcSettings.baseUrl`/`defaultAuth`/`callerId`/`defaultHeaders`, and `RpcRequest.options`
  (`RpcOptions`) for a per-call timeout override.
- `HttpRpc` implements `AutoCloseable`.
