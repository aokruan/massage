package ru.aokruan.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

suspend inline fun <reified T> safeRequest(
    crossinline block: suspend () -> HttpResponse
): ApiOperation<T> {
    return try {
        val response = block()
        if (response.status.isSuccess()) {
            ApiOperation.Success(response.body<T>())
        } else {
            ApiOperation.Failure(
                NetworkError.Http(
                    code = response.status.value,
                    body = runCatching { response.bodyAsText() }.getOrNull()
                )
            )
        }
    } catch (e: CancellationException) {
        throw e // критично: не глотаем отмену
    } catch (e: SerializationException) {
        ApiOperation.Failure(NetworkError.Serialization(e))
    } catch (e: Throwable) {
        ApiOperation.Failure(NetworkError.Network(e))
    }
}