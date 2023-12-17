package org.todo.spacewalk.exceptions

open class BaseException(val statusCode: Int, override val message: String) : Exception()

class ValidationException(message: String) : BaseException(400, message)

class InvalidSystemStateException(message: String) : BaseException(500, message)

class FailedAuthenticationException(message: String) : BaseException(401, message)

class FileException(message: String) : BaseException(400, message)
