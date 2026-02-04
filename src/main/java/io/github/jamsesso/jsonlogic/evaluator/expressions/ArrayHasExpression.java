package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class ArrayHasExpression implements JsonLogicExpression {
	public static final ArrayHasExpression SOME = new ArrayHasExpression(true);
	public static final ArrayHasExpression NONE = new ArrayHasExpression(false);

	private final boolean isSome;

	private ArrayHasExpression(final boolean isSome) {
		this.isSome = isSome;
	}

	@Override
	public String key() {
		return isSome ? "some" : "none";
	}

	@Override
	public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath)
			throws JsonLogicEvaluationException {
		if (arguments.size() != 2) {
			throw new JsonLogicEvaluationException(key() + " expects exactly 2 arguments", jsonPath);
		}

		final Object maybeArray = evaluator.evaluate(arguments.get(0), data, jsonPath + "[0]");

		// Array objects can have null values according to http://jsonlogic.com/
		if (maybeArray == null) {
			if (isSome) {
				return false;
			}
			return true;
		}

		if (!ArrayLike.isArray(maybeArray)) {
			throw new JsonLogicEvaluationException("first argument to " + key() + " must be a valid array", jsonPath + "[0]");
		}

		for (final Object item :ArrayLike.asList(maybeArray)) {
			if(JsonLogic.truthy(evaluator.evaluate(arguments.get(1), item, jsonPath + "[1]"))) {
				return isSome;
			}
		}

		return !isSome;
	}
}
