package com.castle.application.usecase

interface UseCase<in Input, out Output> {
    suspend operator fun invoke(input: Input): Output
}
