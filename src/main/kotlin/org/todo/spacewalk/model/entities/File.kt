package org.todo.spacewalk.model.entities

import javax.persistence.*

@Entity
class File(
    @ManyToOne
    val user: User,
    val name: String,
    var fileSize: String,
    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var binary: FileBinary,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
