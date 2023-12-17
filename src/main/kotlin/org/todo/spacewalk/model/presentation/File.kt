package org.todo.spacewalk.model.presentation

import org.todo.spacewalk.model.entities.File

data class File(
    val id: Long,
    val name: String,
    var fileSize: String,
)

fun File.toPresentation(): org.todo.spacewalk.model.presentation.File {
    return org.todo.spacewalk.model.presentation.File(id!!, name, fileSize)
}
