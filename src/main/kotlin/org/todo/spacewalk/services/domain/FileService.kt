package org.todo.spacewalk.services.domain

import org.springframework.stereotype.Service
import org.todo.spacewalk.exceptions.FileException
import org.todo.spacewalk.model.entities.File
import org.todo.spacewalk.model.entities.FileBinary
import org.todo.spacewalk.model.entities.User
import org.todo.spacewalk.repositories.FileRepository
import javax.transaction.Transactional
import kotlin.jvm.optionals.getOrNull

@Service
class FileService(private val fileRepository: FileRepository) {
    @Transactional
    fun createFile(file: File): File {
        if (doesSameFileExist(file)) {
            throw FileException("동일한 파일명이 이미 존재함")
        }
        return fileRepository.save(file)
    }

    @Transactional
    fun updateFile(
        id: Long,
        user: User,
        binary: FileBinary,
        fileSize: String,
    ): File {
        val prev = findById(id)
        if (prev == null || prev.user.id != user.id) {
            throw FileException("해당 파일이 존재하지 않음")
        }
        prev.binary = binary
        prev.fileSize = fileSize
        return fileRepository.save(prev)
    }

    @Transactional
    fun findByUser(user: User): List<File> {
        return fileRepository.findAllByUser(user)
    }

    @Transactional
    fun deleteFile(
        id: Long,
        user: User,
    ) {
        val file = fileRepository.findById(id).getOrNull()
        if (file == null || file.user.id != user.id) {
            throw FileException("해당 파일이 존재하지 않음")
        }
        fileRepository.delete(file)
    }

    @Transactional
    fun findBinary(
        id: Long,
        user: User,
    ): ByteArray {
        val file = fileRepository.findById(id).getOrNull()
        if (file == null || file.user.id != user.id) {
            throw FileException("해당 파일이 존재하지 않음")
        }
        return file.binary.bytes
    }

    private fun doesSameFileExist(file: File): Boolean {
        return fileRepository.findByNameAndUser(file.name, file.user) != null
    }

    private fun findById(id: Long): File? {
        return fileRepository.findById(id).getOrNull()
    }
}
