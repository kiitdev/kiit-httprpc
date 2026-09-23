package kiit.rpc

/** Headers-like key/value pairs — the `meta` parameter across [RpcClient]'s calls. */
typealias Meta = Map<String, String>

/** Query-string-like key/value pairs — the `args` parameter across [RpcClient]'s calls. */
typealias Args = Map<String, String>
