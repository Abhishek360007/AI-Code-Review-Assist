package com.reviewassist.ai;

import com.reviewassist.common.ApiException;
import org.springframework.http.HttpStatus;

/** The AI provider failed or returned something unusable. Surfaces to the client as 502. */
public class AnalysisException extends ApiException {

    public AnalysisException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public AnalysisException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}
