package io.github.jamsesso.jsonlogic.ast;

import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.PathSegment;

public class JsonLogicParseException extends JsonLogicException {
	private static final long serialVersionUID = 1L;

	public JsonLogicParseException(final String msg, final PathSegment jsonPath) {
		super(msg, jsonPath);
	}

	public JsonLogicParseException(final Throwable cause, final PathSegment jsonPath) {
		super(cause, jsonPath);
	}

	public JsonLogicParseException(final String msg, final Throwable cause, final PathSegment jsonPath) {
		super(msg, cause, jsonPath);
	}
}
