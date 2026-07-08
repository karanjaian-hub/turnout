package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result

/**
 * Base class for all use cases.
 * P = params type, R = return type.
 * Single-responsibility: one use case does exactly one thing.
 */
abstract class UseCase<in P, out R> {
    abstract suspend operator fun invoke(params: P): Result<R>
}

/** For use cases that need no input parameters. */
abstract class NoParamUseCase<out R> {
    abstract suspend operator fun invoke(): Result<R>
}
