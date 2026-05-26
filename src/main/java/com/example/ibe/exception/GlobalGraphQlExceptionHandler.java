package com.example.ibe.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalGraphQlExceptionHandler {

    @GraphQlExceptionHandler(TenantNotFoundException.class)
    public GraphQLError handleTenantNotFound(TenantNotFoundException ex, DataFetchingEnvironment env) {
        log.warn("Tenant lookup failed: {}", ex.getMessage());
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.NOT_FOUND)
                .message(ex.getMessage())
                .extensions(Map.of("code", "TENANT_NOT_FOUND"))
                .build();
    }

    @GraphQlExceptionHandler(PropertyNotFoundException.class)
    public GraphQLError handlePropertyNotFound(PropertyNotFoundException ex, DataFetchingEnvironment env) {
        log.warn("Property lookup failed: {}", ex.getMessage());
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.NOT_FOUND)
                .message(ex.getMessage())
                .extensions(Map.of("code", "PROPERTY_NOT_FOUND"))
                .build();
    }

    @GraphQlExceptionHandler(RoomTypeNotFoundException.class)
    public GraphQLError handleRoomTypeNotFound(RoomTypeNotFoundException ex, DataFetchingEnvironment env) {
        log.warn("Room type lookup failed: {}", ex.getMessage());
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.NOT_FOUND)
                .message(ex.getMessage())
                .extensions(Map.of("code", "ROOM_TYPE_NOT_FOUND"))
                .build();
    }

    @GraphQlExceptionHandler(InvalidTenantConfigException.class)
    public GraphQLError handleInvalidTenantConfig(InvalidTenantConfigException ex, DataFetchingEnvironment env) {
        log.error("Tenant config parsing failed", ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("Tenant configuration is invalid")
                .extensions(Map.of("code", "INVALID_TENANT_CONFIG"))
                .build();
    }

    @GraphQlExceptionHandler(InvalidUuidFormatException.class)
    public GraphQLError handleInvalidUuid(InvalidUuidFormatException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(ex.getMessage())
                .extensions(Map.of("code", "INVALID_UUID_FORMAT"))
                .build();
    }

    @GraphQlExceptionHandler(BadRequestException.class)
    public GraphQLError handleBadRequest(BadRequestException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(ex.getMessage())
                .extensions(Map.of("code", "BAD_REQUEST"))
                .build();
    }

    @GraphQlExceptionHandler(InvalidBookingRequestException.class)
    public GraphQLError handleInvalidBookingRequest(InvalidBookingRequestException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(cleanMessage(ex.getMessage(), extractCode(ex.getMessage(), "INVALID_BOOKING_REQUEST")))
                .extensions(Map.of("code", extractCode(ex.getMessage(), "INVALID_BOOKING_REQUEST")))
                .build();
    }

    @GraphQlExceptionHandler(BookingAvailabilityException.class)
    public GraphQLError handleBookingAvailability(BookingAvailabilityException ex, DataFetchingEnvironment env) {
        log.error("Booking availability check failed", ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message(cleanMessage(ex.getMessage(), "BOOKING_AVAILABILITY_ERROR"))
                .extensions(Map.of("code", "BOOKING_AVAILABILITY_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler(BookingPersistenceException.class)
    public GraphQLError handleBookingPersistence(BookingPersistenceException ex, DataFetchingEnvironment env) {
        log.error("Booking persistence failed", ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message(cleanMessage(ex.getMessage(), "BOOKING_PERSISTENCE_ERROR"))
                .extensions(Map.of("code", "BOOKING_PERSISTENCE_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler(RoomNotAvailableException.class)
    public GraphQLError handleRoomNotAvailable(RoomNotAvailableException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(cleanMessage(ex.getMessage(), "ROOM_NOT_AVAILABLE"))
                .extensions(Map.of("code", "ROOM_NOT_AVAILABLE"))
                .build();
    }

    @GraphQlExceptionHandler(ConcurrentBookingContentionException.class)
    public GraphQLError handleConcurrentBookingContention(ConcurrentBookingContentionException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message("The selected rooms are no longer available. Please refresh availability and try again.")
                .extensions(Map.of("code", "ROOM_NOT_AVAILABLE"))
                .build();
    }

    @GraphQlExceptionHandler(PriceChangedException.class)
    public GraphQLError handlePriceChanged(PriceChangedException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(cleanMessage(ex.getMessage(), "PRICE_CHANGED"))
                .extensions(Map.of("code", "PRICE_CHANGED"))
                .build();
    }

    @GraphQlExceptionHandler(PaymentDeclinedException.class)
    public GraphQLError handlePaymentDeclined(PaymentDeclinedException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(cleanMessage(ex.getMessage(), "PAYMENT_DECLINED"))
                .extensions(Map.of("code", "PAYMENT_DECLINED"))
                .build();
    }

    @GraphQlExceptionHandler(PaymentProcessingException.class)
    public GraphQLError handlePaymentProcessing(PaymentProcessingException ex, DataFetchingEnvironment env) {
        log.error("Payment processing failed", ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message(cleanMessage(ex.getMessage(), "PAYMENT_PROCESSING_ERROR"))
                .extensions(Map.of("code", "PAYMENT_PROCESSING_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler(com.example.ibe.exception.BookingNotFoundException.class)
    public GraphQLError handleBookingNotFound(com.example.ibe.exception.BookingNotFoundException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.NOT_FOUND)
                .message(cleanMessage(ex.getMessage(), "BOOKING_NOT_FOUND"))
                .extensions(Map.of("code", "BOOKING_NOT_FOUND"))
                .build();
    }

    @GraphQlExceptionHandler(PromotionNotFoundException.class)
    public GraphQLError handlePromotionNotFound(PromotionNotFoundException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.NOT_FOUND)
                .message(ex.getMessage())
                .extensions(Map.of("code", "PROMOTION_NOT_FOUND"))
                .build();
    }

    @GraphQlExceptionHandler(InvalidPromotionException.class)
    public GraphQLError handleInvalidPromotion(InvalidPromotionException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(ex.getMessage())
                .extensions(Map.of("code", "INVALID_PROMOTION"))
                .build();
    }

    @GraphQlExceptionHandler(DataProcessingException.class)
    public GraphQLError handleDataProcessing(DataProcessingException ex, DataFetchingEnvironment env) {
        log.error("Data processing error while handling GraphQL request", ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("Failed to process requested data")
                .extensions(Map.of("code", "DATA_PROCESSING_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler(IllegalArgumentException.class)
    public GraphQLError handleIllegalArgument(IllegalArgumentException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(ex.getMessage())
                .extensions(Map.of("code", "BAD_REQUEST"))
                .build();
    }

    @GraphQlExceptionHandler(DataAccessException.class)
    public GraphQLError handleDataAccess(DataAccessException ex, DataFetchingEnvironment env) {
        log.error("Database error while processing GraphQL request", ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("Database operation failed")
                .extensions(Map.of("code", "DATABASE_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler(ConstraintViolationException.class)
    public GraphQLError handleConstraintViolation(ConstraintViolationException ex, DataFetchingEnvironment env) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(message.isBlank() ? "Validation failed" : message)
                .extensions(Map.of("code", "VALIDATION_FAILED"))
                .build();
    }

    @GraphQlExceptionHandler(NullPointerException.class)
    public GraphQLError handleNullPointer(NullPointerException ex, DataFetchingEnvironment env) {
        log.warn("Null value encountered while processing GraphQL request", ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message("Required input or value was missing")
                .extensions(Map.of("code", "NULL_VALUE_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler(org.springframework.validation.BindException.class)
    public GraphQLError handleBindException(org.springframework.validation.BindException ex, DataFetchingEnvironment env) {
        String message = ex.getFieldErrors().stream()
                .map(v -> v.getField() + ": " + v.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message("Validation error: " + (message.isBlank() ? "Invalid input" : message))
                .extensions(Map.of("code", "VALIDATION_FAILED"))
                .build();
    }

    @GraphQlExceptionHandler(ClassCastException.class)
    public GraphQLError handleClassCast(ClassCastException ex, DataFetchingEnvironment env) {
        log.error("Type casting error while processing GraphQL request", ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("Unexpected data type encountered")
                .extensions(Map.of("code", "TYPE_CAST_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler(Exception.class)
    public GraphQLError handleGenericException(Exception ex, DataFetchingEnvironment env) {
        InvalidUuidFormatException invalidUuidCause = findCause(ex, InvalidUuidFormatException.class);
        if (invalidUuidCause != null) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(invalidUuidCause.getMessage())
                    .extensions(Map.of("code", "INVALID_UUID_FORMAT"))
                    .build();
        }

        log.error("Unhandled exception while processing GraphQL request", ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("Unexpected server error")
                .extensions(Map.of("code", "INTERNAL_SERVER_ERROR"))
                .build();
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private String cleanMessage(String message, String codePrefix) {
        if (message == null || message.isBlank()) {
            return "Request could not be processed";
        }

        String prefix = codePrefix + ":";
        return message.startsWith(prefix) ? message.substring(prefix.length()).trim() : message;
    }

    private String extractCode(String message, String defaultCode) {
        if (message == null || message.isBlank()) {
            return defaultCode;
        }

        int separatorIndex = message.indexOf(':');
        if (separatorIndex <= 0) {
            return defaultCode;
        }

        String candidateCode = message.substring(0, separatorIndex).trim();
        return candidateCode.isEmpty() ? defaultCode : candidateCode;
    }
}
