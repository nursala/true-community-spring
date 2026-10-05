package com.truecommunity.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;
import java.util.NoSuchElementException;

/**
 * Global exception handling for server-rendered Thymeleaf (MVC) controllers.
 * Renders the shared "error" view with an appropriate HTTP status and a
 * user-friendly message.
 *
 * Note: @RestController is meta-annotated with @Controller, so this advice can
 * also match REST controllers. It is therefore given the lowest precedence,
 * while the JSON advice (GlobalExceptionHandler) is given the highest, so REST
 * endpoints keep returning JSON and only true MVC errors reach this handler.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice(annotations = Controller.class)
public class GlobalMvcExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalMvcExceptionHandler.class);

    private static final String ERROR_VIEW = "error";

    /**
     * Handles expected missing-resource failures for server-rendered pages.
     *
     * @param ex the missing-resource exception
     * @return shared error view with {@code 404 Not Found}
     */
    @ExceptionHandler({
            StatementNotFoundException.class,
            UserNotFoundException.class,
            CommentNotFoundException.class,
            NoSuchElementException.class
    })
    public ModelAndView handleNotFound(Exception ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return errorView(HttpStatus.NOT_FOUND, "המשאב המבוקש לא נמצא.");
    }

    /**
     * Handles expected authorization failures for server-rendered pages.
     *
     * @param ex the forbidden-access exception
     * @return shared error view with {@code 403 Forbidden}
     */
    @ExceptionHandler({
            AccessDeniedException.class,
            ConsensusAccessDeniedException.class,
            UnauthorizedEditorActionException.class,
            UnauthorizedDiscussionAccessException.class
    })
    public ModelAndView handleForbidden(Exception ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return errorView(HttpStatus.FORBIDDEN, "אין לך הרשאה לבצע פעולה זו.");
    }

    /**
     * Handles expected workflow conflicts for server-rendered pages.
     *
     * @param ex the conflict exception
     * @return shared error view with {@code 409 Conflict}
     */
    @ExceptionHandler({
            EditorDecisionAlreadyExistsException.class,
            InvalidStatementStateException.class,
            StatementAlreadyClaimedException.class,
            VoteAlreadyExistsException.class,
            VotingClosedException.class,
            SpeakerInUseException.class,
            StatementInUseException.class,
            CannotDemoteSelfException.class,
            CannotDemoteProtectedUserException.class,
            CannotDemoteLastEditorException.class
    })
    public ModelAndView handleConflict(RuntimeException ex) {
        log.warn("Workflow conflict: {}", ex.getMessage());
        return errorView(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles invalid client input (e.g. a rejected URL scheme) for server-rendered pages.
     *
     * @param ex the bad-request exception
     * @return shared error view with {@code 400 Bad Request}
     */
    @ExceptionHandler({
            InvalidTopicSelectionException.class,
            InvalidUrlException.class
    })
    public ModelAndView handleBadRequest(RuntimeException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return errorView(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Honors the HTTP status carried by a {@link ResponseStatusException} (e.g. the 403 raised
     * by SSE/fragment discussion gating) instead of letting the generic handler collapse it to
     * {@code 500}. This makes forbidden/not-found controller signals keep their real status.
     *
     * @param ex the response status exception raised by a controller
     * @return shared error view with the carried status
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        log.warn("Response status signal: {}", status);
        String message = ex.getReason() != null ? ex.getReason() : defaultMessageFor(status);
        return errorView(status, message);
    }

    /**
     * Handles database failures with a temporary-unavailable response.
     *
     * @param ex the database exception
     * @return shared error view with {@code 503 Service Unavailable}
     */
    @ExceptionHandler({
            SQLException.class,
            DataAccessException.class
    })
    public ModelAndView handleDatabase(Exception ex) {
        log.error("Database error", ex);
        return errorView(HttpStatus.SERVICE_UNAVAILABLE, "מסד הנתונים אינו זמין כרגע.");
    }

    /**
     * Fallback for unexpected MVC failures.
     *
     * @param ex the unexpected exception
     * @return shared error view with {@code 500 Internal Server Error}
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return errorView(HttpStatus.INTERNAL_SERVER_ERROR, "אירעה שגיאה בלתי צפויה.");
    }

    private ModelAndView errorView(HttpStatus status, String message) {
        ModelAndView modelAndView = new ModelAndView(ERROR_VIEW);
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("error", message);
        return modelAndView;
    }

    /** Returns a localized fallback message for a carried status without a reason phrase. */
    private String defaultMessageFor(HttpStatus status) {
        return switch (status) {
            case FORBIDDEN -> "אין לך הרשאה לבצע פעולה זו.";
            case NOT_FOUND -> "המשאב המבוקש לא נמצא.";
            case CONFLICT -> "הפעולה מתנגשת עם המצב הנוכחי.";
            case BAD_REQUEST -> "הבקשה אינה תקינה.";
            default -> "אירעה שגיאה בלתי צפויה.";
        };
    }
}
