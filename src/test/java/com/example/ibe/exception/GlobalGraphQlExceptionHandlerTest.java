package com.example.ibe.exception;

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.graphql.execution.ErrorType;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalGraphQlExceptionHandlerTest {

    private final GlobalGraphQlExceptionHandler handler = new GlobalGraphQlExceptionHandler();

    @Mock
    private DataFetchingEnvironment env;

    @Mock
    private ConstraintViolation<Object> violationOne;

    @Mock
    private ConstraintViolation<Object> violationTwo;

    @Mock
    private ExecutionStepInfo executionStepInfo;

    @BeforeEach
    void setup() {
        Field field = Field.newField("testField")
                .sourceLocation(new SourceLocation(1, 1))
                .build();
        when(env.getField()).thenReturn(field);
        when(executionStepInfo.getPath()).thenReturn(ResultPath.rootPath());
        when(env.getExecutionStepInfo()).thenReturn(executionStepInfo);
    }

    @Test
    void handleTenantNotFoundReturnsNotFoundError() {
        GraphQLError error = handler.handleTenantNotFound(new TenantNotFoundException("Tenant not found: x"), env);

        assertEquals(ErrorType.NOT_FOUND, error.getErrorType());
        assertEquals("Tenant not found: x", error.getMessage());
        assertEquals("TENANT_NOT_FOUND", error.getExtensions().get("code"));
    }

    @Test
    void handlePropertyNotFoundReturnsNotFoundError() {
        GraphQLError error = handler.handlePropertyNotFound(new PropertyNotFoundException("Property not found: y"),
                env);

        assertEquals(ErrorType.NOT_FOUND, error.getErrorType());
        assertEquals("Property not found: y", error.getMessage());
        assertEquals("PROPERTY_NOT_FOUND", error.getExtensions().get("code"));
    }

    @Test
    void handleInvalidTenantConfigReturnsInternalError() {
        GraphQLError error = handler.handleInvalidTenantConfig(
                new InvalidTenantConfigException("bad", new RuntimeException("cause")), env);

        assertEquals(ErrorType.INTERNAL_ERROR, error.getErrorType());
        assertEquals("Tenant configuration is invalid", error.getMessage());
        assertEquals("INVALID_TENANT_CONFIG", error.getExtensions().get("code"));
    }

    @Test
    void handleInvalidUuidReturnsBadRequestError() {
        GraphQLError error = handler.handleInvalidUuid(
                new InvalidUuidFormatException("propertyId must be a valid UUID", new IllegalArgumentException()), env);

        assertEquals(ErrorType.BAD_REQUEST, error.getErrorType());
        assertEquals("propertyId must be a valid UUID", error.getMessage());
        assertEquals("INVALID_UUID_FORMAT", error.getExtensions().get("code"));
    }

    @Test
    void handleIllegalArgumentReturnsBadRequestError() {
        GraphQLError error = handler.handleIllegalArgument(new IllegalArgumentException("invalid input"), env);

        assertEquals(ErrorType.BAD_REQUEST, error.getErrorType());
        assertEquals("invalid input", error.getMessage());
        assertEquals("BAD_REQUEST", error.getExtensions().get("code"));
    }

    @Test
    void handleDataAccessReturnsInternalError() {
        GraphQLError error = handler.handleDataAccess(new DataRetrievalFailureException("db"), env);

        assertEquals(ErrorType.INTERNAL_ERROR, error.getErrorType());
        assertEquals("Database operation failed", error.getMessage());
        assertEquals("DATABASE_ERROR", error.getExtensions().get("code"));
    }

    @Test
    void handleConstraintViolationReturnsAggregatedMessage() {
        when(violationOne.getMessage()).thenReturn("tenant must not be blank");
        when(violationTwo.getMessage()).thenReturn("propertyId must not be blank");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violationOne, violationTwo));
        GraphQLError error = handler.handleConstraintViolation(ex, env);

        assertEquals(ErrorType.BAD_REQUEST, error.getErrorType());
        assertEquals("VALIDATION_FAILED", error.getExtensions().get("code"));
        assertNotNull(error.getMessage());
    }

    @Test
    void handleConstraintViolationReturnsDefaultMessageWhenBlank() {
        when(violationOne.getMessage()).thenReturn("   ");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violationOne));
        GraphQLError error = handler.handleConstraintViolation(ex, env);

        assertEquals(ErrorType.BAD_REQUEST, error.getErrorType());
        assertEquals("Validation failed", error.getMessage());
        assertEquals("VALIDATION_FAILED", error.getExtensions().get("code"));
    }

    @Test
    void handleNullPointerReturnsBadRequestError() {
        GraphQLError error = handler.handleNullPointer(new NullPointerException("missing"), env);

        assertEquals(ErrorType.BAD_REQUEST, error.getErrorType());
        assertEquals("Required input or value was missing", error.getMessage());
        assertEquals("NULL_VALUE_ERROR", error.getExtensions().get("code"));
    }

    @Test
    void handleClassCastReturnsInternalError() {
        GraphQLError error = handler.handleClassCast(new ClassCastException("bad cast"), env);

        assertEquals(ErrorType.INTERNAL_ERROR, error.getErrorType());
        assertEquals("Unexpected data type encountered", error.getMessage());
        assertEquals("TYPE_CAST_ERROR", error.getExtensions().get("code"));
    }

    @Test
    void handleGenericExceptionReturnsInternalError() {
        GraphQLError error = handler.handleGenericException(new RuntimeException("unexpected"), env);

        assertEquals(ErrorType.INTERNAL_ERROR, error.getErrorType());
        assertEquals("Unexpected server error", error.getMessage());
        assertEquals("INTERNAL_SERVER_ERROR", error.getExtensions().get("code"));
    }

    @Test
    void handleGenericExceptionUnwrapsInvalidUuidCause() {
        RuntimeException wrapped = new RuntimeException(
                "wrapped",
                new InvalidUuidFormatException("propertyId must be a valid UUID", new IllegalArgumentException()));

        GraphQLError error = handler.handleGenericException(wrapped, env);

        assertEquals(ErrorType.BAD_REQUEST, error.getErrorType());
        assertEquals("propertyId must be a valid UUID", error.getMessage());
        assertEquals("INVALID_UUID_FORMAT", error.getExtensions().get("code"));
    }
}
