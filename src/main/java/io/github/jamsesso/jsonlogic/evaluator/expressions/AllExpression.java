package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class AllExpression implements JsonLogicExpression {
	public static final AllExpression INSTANCE = new AllExpression();

	private AllExpression() {
		// Use INSTANCE instead.
	}

	@Override
	public String key() {
		return "all";
	}

	@Override
	public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath)
			throws JsonLogicEvaluationException {
		if (arguments.size() != 2) {
			throw new JsonLogicEvaluationException("all expects exactly 2 arguments", jsonPath);
		}

		final Object maybeArray = evaluator.evaluate(arguments.get(0), data, jsonPath + "[0]");

		if (maybeArray == null) {
			return false;
		}

		if (!ArrayLike.isArray(maybeArray)) {
			throw new JsonLogicEvaluationException("first argument to all must be a valid array", jsonPath);
		}

		final var array = ArrayLike.asList(maybeArray);

		if (array.size() < 1) {
			return false;
		}

		final var index = 1;
		for (final Object item : array) {
			if(!JsonLogic.truthy(evaluator.evaluate(arguments.get(1), item,  String.format("%s[%d]", jsonPath, index)))) {
				return false;
			}
		}

		return true;
	}
}
