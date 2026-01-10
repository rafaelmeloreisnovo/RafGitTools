package com.rafgittools.domain.usecase

/**
 * Base Use Case interface following Clean Architecture principles.
 * 
 * Use cases encapsulate business logic and represent specific user actions
 * or system operations. They are independent of any framework or external
 * agency.
 * 
 * @param P Parameters type for the use case
 * @param R Return type of the use case
 */
interface UseCase<in P, out R> {
    /**
     * Execute the use case with the given parameters.
     * 
     * @param params The input parameters for the use case
     * @return Result of the use case execution
     */
    suspend operator fun invoke(params: P): R
}

/**
 * Use Case without parameters.
 * 
 * @param R Return type of the use case
 */
interface NoParamUseCase<out R> {
    /**
     * Execute the use case without parameters.
     * 
     * @return Result of the use case execution
     */
    suspend operator fun invoke(): R
}

/**
 * Flow-based Use Case for reactive streams.
 * 
 * @param P Parameters type for the use case
 * @param R Return type of the flow emissions
 */
interface FlowUseCase<in P, out R> {
    /**
     * Execute the use case and return a Flow.
     * 
     * @param params The input parameters for the use case
     * @return Flow of results
     */
    operator fun invoke(params: P): kotlinx.coroutines.flow.Flow<R>
}
