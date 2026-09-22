package kiit.rpc

/** Authorization schemes HttpRpc resolves into the request's Authorization header. */
sealed class Auth {
    data class Basic(val name: String, val pswd: String) : Auth()

    data class Bearer(val token: String) : Auth()
}
