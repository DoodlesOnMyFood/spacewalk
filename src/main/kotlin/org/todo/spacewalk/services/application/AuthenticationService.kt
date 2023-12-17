package org.todo.spacewalk.services.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.todo.spacewalk.config.auth.JwtTokenUtil
import org.todo.spacewalk.exceptions.FailedAuthenticationException
import org.todo.spacewalk.model.entities.User
import org.todo.spacewalk.model.presentation.LoginRequest
import org.todo.spacewalk.model.presentation.LoginResponse
import org.todo.spacewalk.model.presentation.SignUpRequest
import org.todo.spacewalk.services.domain.UserService

@Service
class AuthenticationService(
    private val jwtTokenUtil: JwtTokenUtil,
    private val passwordEncoder: PasswordEncoder,
    private val userService: UserService,
) {
    suspend fun loginUser(loginRequest: LoginRequest): LoginResponse {
        val user =
            withContext(Dispatchers.IO) {
                userService.findUserByUsername(loginRequest.username)
            } ?: throw FailedAuthenticationException("사용자가 존재하지 않습니다.")
        if (!passwordEncoder.matches(loginRequest.password, user.password)) {
            throw FailedAuthenticationException("해당 인증 정보에 대한 사용자가 존재하지 않습니다.")
        }
        return LoginResponse(jwtTokenUtil.generateToken(user))
    }

    suspend fun signUpUser(signUpRequest: SignUpRequest) {
        withContext(Dispatchers.IO) {
            userService.createNewUser(User(signUpRequest.username, passwordEncoder.encode(signUpRequest.password)))
        }
    }
}
