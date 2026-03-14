package ru.aokruan.network

inline fun <T, R> ApiOperation<T>.mapSuccess(transform: (T) -> R): ApiOperation<R> = when (this) {
    is ApiOperation.Success -> ApiOperation.Success(transform(data))
    is ApiOperation.Failure -> this
}

suspend inline fun <T> ApiOperation<T>.onSuccess(block: suspend (T) -> Unit): ApiOperation<T> {
    if (this is ApiOperation.Success) block(data)
    return this
}

inline fun <T> ApiOperation<T>.onFailure(block: (NetworkError) -> Unit): ApiOperation<T> {
    if (this is ApiOperation.Failure) block(error)
    return this
}