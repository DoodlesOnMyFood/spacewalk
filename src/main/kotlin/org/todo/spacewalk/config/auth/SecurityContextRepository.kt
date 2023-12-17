package org.todo.spacewalk.config.auth

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class SecurityContextRepository(private val authenticationManager: AuthenticationManager) :
    ServerSecurityContextRepository {
    override fun save(
        exchange: ServerWebExchange?,
        context: SecurityContext?,
    ): Mono<Void> {
        return Mono.empty()
    }

    override fun load(exchange: ServerWebExchange): Mono<SecurityContext> {
        return mono { handleLoad(exchange) }
    }

    private suspend fun handleLoad(exchange: ServerWebExchange): SecurityContext? {
        val request = exchange.request
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val authToken = authHeader.substring(7)
            val token = UsernamePasswordAuthenticationToken(authToken, authToken)
            val auth = authenticationManager.authenticate(token).awaitSingle()
            SecurityContextImpl(auth)
        } else {
            null
        }
    }
}
