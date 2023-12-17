package org.todo.spacewalk.routers

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.todo.spacewalk.model.presentation.LoginRequest
import org.todo.spacewalk.model.presentation.SignUpRequest
import org.todo.spacewalk.services.application.AuthenticationService
import java.net.URI
import java.net.URLEncoder

@Component
class AuthenticationRouter(
    private val authenticationService: AuthenticationService,
) {
    suspend fun authenticate(serverRequest: ServerRequest): ServerResponse {
        val request = serverRequest.awaitBody<LoginRequest>()
        val response = authenticationService.loginUser(request)
        return ServerResponse.ok().bodyValueAndAwait(response)
    }

    suspend fun signUp(serverRequest: ServerRequest): ServerResponse {
        val request = serverRequest.awaitBody<SignUpRequest>()
        authenticationService.signUpUser(request)
        val encodedUsername = URLEncoder.encode(request.username, Charsets.UTF_8)
        val uri = URI.create("/$encodedUsername")
        return ServerResponse.created(uri).buildAndAwait()
    }
}
