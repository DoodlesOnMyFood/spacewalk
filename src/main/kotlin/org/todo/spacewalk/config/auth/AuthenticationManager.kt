package org.todo.spacewalk.config.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Component
import org.todo.spacewalk.exceptions.FailedAuthenticationException
import org.todo.spacewalk.model.entities.User
import org.todo.spacewalk.repositories.UserRepository
import reactor.core.publisher.Mono

@Component
class AuthenticationManager(
    private val jwtTokenUtil: JwtTokenUtil,
    private val userRepo: UserRepository,
) : ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        return mono {
            handleAuthenticate(authentication)
        }
    }

    private suspend fun handleAuthenticate(authentication: Authentication): Authentication {
        if (authentication.principal == null) {
            throw FailedAuthenticationException("")
        }
        val claims = jwtTokenUtil.getClaims(authentication.principal as String)
        val user =
            withContext(Dispatchers.IO) {
                userRepo.findByUsername(claims.subject)
            }
        if (user == null) {
            throw FailedAuthenticationException("존재하지 않는 유저!")
        }
        return UserAuthentication(user)
    }
}

class UserAuthentication(private val user: User) : Authentication {
    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        TODO("Not yet implemented")
    }

    override fun getCredentials(): Any {
        TODO("Not yet implemented")
    }

    override fun getDetails(): Any {
        TODO("Not yet implemented")
    }

    override fun getPrincipal(): Any {
        return user
    }

    override fun isAuthenticated(): Boolean {
        return true
    }

    override fun setAuthenticated(isAuthenticated: Boolean) {
        TODO("Not yet implemented")
    }
}
