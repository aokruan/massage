package ru.aokruan.network

sealed interface ApiOperation<out T> {
    data class Success<T>(val data: T) : ApiOperation<T>
    data class Failure(val error: NetworkError) : ApiOperation<Nothing>
}

sealed interface NetworkError {
    data class Http(val code: Int, val body: String?) : NetworkError
    data class Serialization(val cause: Throwable) : NetworkError
    data class Network(val cause: Throwable) : NetworkError
    data class Unknown(val cause: Throwable) : NetworkError
}