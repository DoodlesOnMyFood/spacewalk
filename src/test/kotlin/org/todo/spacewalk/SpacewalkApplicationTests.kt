package org.todo.spacewalk

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.todo.spacewalk.model.presentation.GetFilesResponse
import org.todo.spacewalk.model.presentation.LoginRequest
import org.todo.spacewalk.model.presentation.LoginResponse
import org.todo.spacewalk.model.presentation.SignUpRequest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpacewalkApplicationTests(
    private val objectMapper: ObjectMapper,
    @LocalServerPort
    private var host: Int = 0,
) : FreeSpec() {
    private val username = "test"
    private val password = "password"
    private var bearerToken: String = ""
    private val classLoader = SpacewalkApplicationTests::class.java.classLoader
    private val webTestClient =
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:$host")
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs { clientCodecConfigurer ->
                        clientCodecConfigurer.defaultCodecs().apply {
                            clientCodecConfigurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)
                            jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
                            jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                        }
                    }
                    .build(),
            )
            .build()

    init {
        "# 회원가입" - {
            "성공하면 201 상태코드 돌려준다" {
                webTestClient.post()
                    .uri("/signup")
                    .bodyValue(SignUpRequest(username, password))
                    .exchange()
                    .expectStatus().isCreated
            }
            "이미 존재하는 유저의 경우 실패한다" {
                webTestClient.post()
                    .uri("/signup")
                    .bodyValue(SignUpRequest(username, "123456"))
                    .exchange()
                    .expectStatus().is4xxClientError
            }
        }
        "# 로그인" - {
            "올바른 인증정보 보낼시 인증 토큰 발급 받는다" {
                webTestClient.post()
                    .uri("/login")
                    .bodyValue(LoginRequest(username, password))
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .consumeWith {
                        val token: LoginResponse = objectMapper.readValue(it.responseBody!!)
                        bearerToken = "Bearer ${token.token}"
                    }
            }
            "올바르지 않을 경우 실패한다" {
                webTestClient.post()
                    .uri("/login")
                    .bodyValue(LoginRequest(username, "123456"))
                    .exchange()
                    .expectStatus().isUnauthorized
            }
        }
        "# 파일" - {
            "파일 없는 경우 빈 행렬 받는다" {
                webTestClient.get()
                    .uri("/files")
                    .header("Authorization", bearerToken)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .consumeWith {
                        val response: GetFilesResponse = objectMapper.readValue(it.responseBody!!)
                        response.files.size shouldBe 0
                    }
            }
            "파일 업로드 가능해야 한다" {
                val files =
                    listOf(
                        classLoader.getResource("sample1.png"),
                        classLoader.getResource("sample2.jpeg"),
                        classLoader.getResource("sample3.webp"),
                        classLoader.getResource("sample4.jpg"),
                    )
                files.forEach {
                    val file = FileSystemResource(it.path)
                    webTestClient.post()
                        .uri("/file/upload")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("Authorization", bearerToken)
                        .body(BodyInserters.fromMultipartData("file", file))
                        .exchange()
                        .expectStatus().isCreated
                }
                val filesResponse = getFiles()
                filesResponse.files.size shouldBe 4
            }
            "파일 있는 경우 조회가 되어야한다" {
                val filesResponse = getFiles()
                val getFile = filesResponse.files.first()
                val localFile = classLoader.getResourceAsStream(getFile.name).readAllBytes()
                webTestClient.get()
                    .uri("/file/${getFile.id}")
                    .header("Authorization", bearerToken)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .consumeWith {
                        localFile shouldBe it.responseBody
                    }
            }
            "파일 데이터 업데이트가 가능하다" {
                val filesResponse = getFiles()
                val fromFile = filesResponse.files[1]
                val localFile = FileSystemResource(classLoader.getResource(fromFile.name).path)
                val localFileBytes = classLoader.getResourceAsStream(fromFile.name).readAllBytes()
                webTestClient.patch()
                    .uri("/file/${filesResponse.files.first().id}")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("Authorization", bearerToken)
                    .body(BodyInserters.fromMultipartData("file", localFile))
                    .exchange()
                    .expectStatus().isOk
                webTestClient.get()
                    .uri("/file/${filesResponse.files.first().id}")
                    .header("Authorization", bearerToken)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .consumeWith {
                        localFileBytes shouldBe it.responseBody
                    }
            }
            "파일 제거가 가능하다" {
                val filesResponse = getFiles()
                filesResponse.files.size shouldBe 4
                webTestClient.delete()
                    .uri("/file/${filesResponse.files.first().id}")
                    .header("Authorization", bearerToken)
                    .exchange()
                    .expectStatus().isOk
                val updatedResponse = getFiles()
                updatedResponse.files.size shouldBe 3
            }
        }
    }

    private suspend fun getFiles(): GetFilesResponse {
        var response: GetFilesResponse? = null
        webTestClient.get()
            .uri("/files")
            .header("Authorization", bearerToken)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith {
                response = objectMapper.readValue(it.responseBody!!)
            }
        return response!!
    }
}
