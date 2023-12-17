package org.todo.spacewalk.routers

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter
import org.todo.spacewalk.exceptions.BaseException
import org.todo.spacewalk.model.presentation.ErrorResponse

@Configuration
class RouterConfig(
    private val authenticationRouter: AuthenticationRouter,
    private val fileRouter: FileRouter,
) {
    @Bean
    fun authenticationRouterFunction() =
        coRouter {
            POST("/login", exceptionHandler(authenticationRouter::authenticate))
            POST("/signup", exceptionHandler(authenticationRouter::signUp))
        }

    @Bean
    fun fileRouterFunction() =
        coRouter {
            GET("/files", exceptionHandler(fileRouter::getFiles))
            POST("/file/upload", exceptionHandler(fileRouter::uploadFile))
            GET("/file/{id}", exceptionHandler(fileRouter::getFile))
            PATCH("/file/{id}", exceptionHandler(fileRouter::updateFile))
            DELETE("/file/{id}", exceptionHandler(fileRouter::deleteFile))
        }

    private fun exceptionHandler(func: suspend (ServerRequest) -> ServerResponse): suspend (ServerRequest) -> ServerResponse {
        return { request: ServerRequest ->
            runCatching {
                func(request)
            }.getOrElse {
                val cause = it::class.simpleName!!
                val message = it.message ?: ""
                val response = ErrorResponse(cause, message)
                if (it is BaseException) {
                    ServerResponse.status(it.statusCode).bodyValueAndAwait(response)
                } else {
                    it.printStackTrace()
                    ServerResponse.status(500).bodyValueAndAwait(response)
                }
            }
        }
    }
}
