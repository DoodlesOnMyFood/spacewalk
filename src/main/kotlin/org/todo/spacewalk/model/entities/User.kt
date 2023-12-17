package org.todo.spacewalk.model.entities

import javax.persistence.*

@Entity
@Table(name = "\"USER\"")
class User(
    val username: String,
    val password: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany
    val files: List<File> = listOf()
}
