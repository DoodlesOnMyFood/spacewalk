package org.todo.spacewalk.routers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.todo.spacewalk.exceptions.FileException
import org.todo.spacewalk.services.application.FileHandlerService
import reactor.core.publisher.Flux
import java.io.ByteArrayOutputStream
import java.net.URI
import kotlin.math.min

@Component
class FileRouter(
    private val fileHandlerService: FileHandlerService,
) {
    private val MAX_UPLOAD_SIZE = 10 * 1024 * 1024

    suspend fun getFile(serverRequest: ServerRequest): ServerResponse {
        val id = serverRequest.pathVariable("id").toLongOrNull() ?: throw FileException("해당 파일이 존재하지 않음")
        val response = byteArrayToDataBufferFlow(fileHandlerService.getFile(id))
        return ServerResponse.ok().bodyAndAwait(response)
    }

    suspend fun getFiles(serverRequest: ServerRequest): ServerResponse {
        val response = fileHandlerService.getFiles()
        return ServerResponse.ok().bodyValueAndAwait(response)
    }

    suspend fun uploadFile(serverRequest: ServerRequest): ServerResponse {
        val parts = serverRequest.multipartData().awaitFirst()
        val filePart = parts["file"]?.first() as? FilePart
        if (filePart != null) {
            val fileName = filePart.filename()
            val dataBufferFlux = filePart.content()
            val array = processFileDataBuffer(dataBufferFlux)
            val id = fileHandlerService.uploadFile(fileName, array)
            val uri = URI.create("/file/$id")
            return ServerResponse.created(uri).bodyValueAndAwait("File processed: $fileName")
        }
        return ServerResponse.badRequest().buildAndAwait()
    }

    suspend fun updateFile(serverRequest: ServerRequest): ServerResponse {
        val id = serverRequest.pathVariable("id").toLongOrNull() ?: throw FileException("해당 파일이 존재하지 않음")
        val parts = serverRequest.multipartData().awaitFirst()
        val filePart = parts["file"]?.first() as? FilePart
        if (filePart != null) {
            val dataBufferFlux = filePart.content()
            val array = processFileDataBuffer(dataBufferFlux)
            fileHandlerService.updateFile(id, array)
            return ServerResponse.ok().bodyValueAndAwait("File updated")
        }
        return ServerResponse.badRequest().buildAndAwait()
    }

    suspend fun deleteFile(serverRequest: ServerRequest): ServerResponse {
        val id = serverRequest.pathVariable("id").toLongOrNull() ?: throw FileException("해당 파일이 존재하지 않음")
        fileHandlerService.deleteFile(id)
        return ServerResponse.ok().buildAndAwait()
    }

    private suspend fun processFileDataBuffer(dataBufferFlux: Flux<DataBuffer>): ByteArray {
        var totalSize = 0L

        return dataBufferFlux.asFlow().fold(ByteArrayOutputStream()) { outputStream, dataBuffer ->
            totalSize += dataBuffer.readableByteCount()
            if (totalSize > MAX_UPLOAD_SIZE) {
                DataBufferUtils.release(dataBuffer)
                throw FileException("파일 크기 10MB 초과")
            }

            val byteArray = ByteArray(dataBuffer.readableByteCount())
            dataBuffer.read(byteArray)
            outputStream.write(byteArray)
            DataBufferUtils.release(dataBuffer)

            outputStream
        }.toByteArray()
    }

    private fun byteArrayToDataBufferFlow(
        byteArray: ByteArray,
        chunkSize: Int = 4096,
    ): Flow<DataBuffer> {
        val dataBufferFactory: DataBufferFactory = DefaultDataBufferFactory.sharedInstance

        return flow {
            var position = 0
            while (position < byteArray.size) {
                val remaining = byteArray.size - position
                val bufferSize = min(chunkSize, remaining)
                val buffer = dataBufferFactory.allocateBuffer(bufferSize)
                buffer.write(byteArray, position, bufferSize)
                position += bufferSize
                emit(buffer)
            }
        }
    }
}
