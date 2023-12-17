package org.todo.spacewalk.services.domain

import org.springframework.stereotype.Service
import org.todo.spacewalk.exceptions.FailedAuthenticationException
import org.todo.spacewalk.model.entities.User
import org.todo.spacewalk.repositories.UserRepository
import javax.transaction.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun findUserByUsername(username: String): User? {
        return userRepository.findByUsername(username)
    }

    @Transactional
    fun createNewUser(user: User) {
        if (userRepository.findByUsername(user.username) != null) {
            throw FailedAuthenticationException("해당 유저명은 이미 존재함")
        }
        userRepository.save(user)
    }
}
