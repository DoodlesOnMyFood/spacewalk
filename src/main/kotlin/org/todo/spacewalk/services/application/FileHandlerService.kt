package org.todo.spacewalk.services.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import org.todo.spacewalk.model.entities.File
import org.todo.spacewalk.model.entities.FileBinary
import org.todo.spacewalk.model.entities.User
import org.todo.spacewalk.model.presentation.GetFilesResponse
import org.todo.spacewalk.model.presentation.toPresentation
import org.todo.spacewalk.services.domain.FileService
import kotlin.math.log10
import kotlin.math.pow

@Service
class FileHandlerService(
    private val fileService: FileService,
) {
    suspend fun getFile(id: Long): ByteArray {
        val user = getPrincipal()
        return withContext(Dispatchers.IO) {
            fileService.findBinary(id, user)
        }
    }

    suspend fun getFiles(): GetFilesResponse {
        val user = getPrincipal()
        val files =
            withContext(Dispatchers.IO) {
                fileService.findByUser(user)
            }
        return GetFilesResponse(files.map { it.toPresentation() })
    }

    suspend fun uploadFile(
        name: String,
        bytes: ByteArray,
    ): Long {
        val user = getPrincipal()
        val binary = FileBinary(bytes)
        val file = File(user, name, formatFileSize(bytes.size), binary)
        val saved =
            withContext(Dispatchers.IO) {
                fileService.createFile(file)
            }
        return saved.id!!
    }

    suspend fun updateFile(
        id: Long,
        bytes: ByteArray,
    ): Long {
        val user = getPrincipal()
        val binary = FileBinary(bytes)
        val fileSize = formatFileSize(bytes.size)
        val saved =
            withContext(Dispatchers.IO) {
                fileService.updateFile(id, user, binary, fileSize)
            }
        return saved.id!!
    }

    suspend fun deleteFile(id: Long) {
        val user = getPrincipal()
        withContext(Dispatchers.IO) {
            fileService.deleteFile(id, user)
        }
    }

    private fun formatFileSize(sizeInBytes: Int): String {
        if (sizeInBytes <= 0) return "0 Bytes"
        val units = arrayOf("Bytes", "KB", "MB", "GB", "TB", "PB", "EB")
        val digitGroups = (log10(sizeInBytes.toDouble()) / log10(1024.0)).toInt()
        return String.format("%.2f %s", sizeInBytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }

    private suspend fun getPrincipal(): User {
        val context = ReactiveSecurityContextHolder.getContext().awaitSingle()
        return context.authentication.principal as (User)
    }
}
