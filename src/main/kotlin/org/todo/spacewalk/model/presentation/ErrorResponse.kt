package org.todo.spacewalk.model.presentation

data class ErrorResponse(
    val cause: String,
    val message: String,
)
