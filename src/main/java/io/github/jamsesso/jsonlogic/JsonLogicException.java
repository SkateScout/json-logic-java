package io.github.jamsesso.jsonlogic;

public class JsonLogicException extends Exception {
	private static final long serialVersionUID = 1L;
	private PathSegment jsonPath;

	@SuppressWarnings("unused")
	private JsonLogicException() {
		// The default constructor should not be called for exceptions. A reason must be provided.
	}

	public JsonLogicException(final String msg, final PathSegment jsonPath) {
		super(msg);
		this.jsonPath = jsonPath;
	}

	public JsonLogicException(final Throwable cause, final PathSegment jsonPath) {
		super(cause);
		this.jsonPath = jsonPath;
	}

	public JsonLogicException(final String msg, final Throwable cause, final PathSegment jsonPath) {
		super(msg, cause);
		this.jsonPath = jsonPath;
	}

	public String getJsonPath() { return jsonPath.toString(); }
}