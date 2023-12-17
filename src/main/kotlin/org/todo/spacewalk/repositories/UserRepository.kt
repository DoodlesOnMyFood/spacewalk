package org.todo.spacewalk.repositories

import org.springframework.data.repository.CrudRepository
import org.todo.spacewalk.model.entities.User

interface UserRepository : CrudRepository<User, Long> {
    fun findByUsername(username: String): User?
}
