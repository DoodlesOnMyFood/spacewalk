package org.todo.spacewalk.config.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import org.todo.spacewalk.model.entities.User
import java.util.*

@Component
class JwtTokenUtil {
    private val secretKey =
        Keys.hmacShaKeyFor("vnV7j6pYr13UDVK5SZ6ickoVcKCkvhzUG32AnzJbvNWaAVzCRoTyKfo3XOuAoKK".toByteArray())
    private val parser = Jwts.parser().verifyWith(secretKey).build()
    private val jwtExpirationInMs = 3600000

    fun generateToken(user: User): String {
        val createdDate = Date()
        val expirationDate = Date(createdDate.time + jwtExpirationInMs)

        return Jwts.builder()
            .signWith(secretKey)
            .claims().subject(user.username)
            .issuedAt(createdDate)
            .expiration(expirationDate)
            .and().compact()
    }

    fun getClaims(token: String): Claims {
        return parser.parseSignedClaims(token).payload
    }
}
