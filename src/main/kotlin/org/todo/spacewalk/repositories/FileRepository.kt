package org.todo.spacewalk.repositories

import org.springframework.data.repository.CrudRepository
import org.todo.spacewalk.model.entities.File
import org.todo.spacewalk.model.entities.User

interface FileRepository : CrudRepository<File, Long> {
    fun findByNameAndUser(
        name: String,
        user: User,
    ): File?

    fun findAllByUser(user: User): List<File>
}
