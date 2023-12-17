package org.todo.spacewalk.model.presentation

data class LoginRequest(
    val username: String,
    val password: String,
)
