package com.smartblog.aop;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.smartblog.application.util.PerformanceBenchmark;
/**
 * Aspect providing cross-cutting concerns used in Lab 5.
 *
 * - Logs method entry/exit for service and repository layers
 * - Measures execution time using {@link PerformanceBenchmark}
 */
@Component
@Aspect
public class CrossCuttingAspect {

    private static final Logger LOG = LoggerFactory.getLogger(CrossCuttingAspect.class);

    private final PerformanceBenchmark benchmark = new PerformanceBenchmark();

    /**
     * Pointcut: Targets service layer and repository layer methods.
     * Includes JPA repositories which are called by GraphQL controllers.
     */
    @Pointcut("execution(* com.smartblog.application.service..*(..)) || execution(* com.smartblog.infrastructure.repository.jpa..*(..))")
    public void serviceLayer() {}

    @Before("serviceLayer()")
    /** Log method entry with arguments. */
    public void logMethodEntry(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        LOG.info("AOP @Before: Entering method {}.{} with arguments {}", className, methodName, Arrays.toString(args));
    }

    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    /** Log method exit and a short result summary. */
    public void logMethodExit(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String resultInfo = formatResult(result);
        LOG.info("AOP @AfterReturning: Exiting method {}.{} with result {}", className, methodName, resultInfo);
    }

    @Around("serviceLayer()")
    /**
     * Measure execution time of the intercepted method and record it in the
     * {@link PerformanceBenchmark} instance.
     */
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        String className = pjp.getSignature().getDeclaringTypeName();
        String methodName = pjp.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        LOG.debug("[AOP @Around] Starting performance measurement for {}", fullMethodName);

        try {
            Object result = benchmark.record(fullMethodName, () -> {
                try {
                    return pjp.proceed();
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            });

            return result;
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            if (cause != null) {
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                throw cause;
            }
            throw ex;
        }
    }

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    /** Log exceptions thrown by service/repository methods. */
    public void logException(JoinPoint joinPoint, Throwable ex) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        LOG.error("[AOP @AfterThrowing] Exception in {}.{}: {} - {}",
                className, methodName, ex.getClass().getSimpleName(), ex.getMessage());
    }

    /**
     * Format a concise representation of a result value for logging.
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        // For collections, show type and size instead of entire contents
        if (result instanceof java.util.Collection<?> collection) {
            return String.format("Collection<%s>[size=%d]",
                    collection.isEmpty() ? "?" : collection.iterator().next().getClass().getSimpleName(),
                    collection.size());
        }

        // For other objects, use toString() but limit length
        String str = result.toString();
        if (str.length() > 100) {
            return str.substring(0, 97) + "...";
        }
        return str;
    }

    /** Return the current benchmark report. */
    public PerformanceBenchmark.BenchmarkReport getBenchmarkReport() {
        return benchmark.generateReport();
    }

    /** Reset all recorded benchmarks. */
    public void resetBenchmarks() {
        LOG.info("[AOP] Resetting performance benchmarks");
        benchmark.reset();
    }

    /** Return the number of recorded benchmark entries. */
    public int getBenchmarkCount() {
        return benchmark.size();
    }
}
