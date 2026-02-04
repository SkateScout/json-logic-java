package io.github.jamsesso.jsonlogic.evaluator;

import io.github.jamsesso.jsonlogic.JsonLogicException;

public class JsonLogicEvaluationException extends JsonLogicException {
	private static final long serialVersionUID = 1L;
	public JsonLogicEvaluationException(final String msg, final String jsonPath) { super(msg, jsonPath); }
	public JsonLogicEvaluationException(final Throwable cause, final String jsonPath) { super(cause, jsonPath); }
	public JsonLogicEvaluationException(final String msg, final Throwable cause, final String jsonPath) { super(msg, cause, jsonPath); }
}
