package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.ArrayList;
import java.util.List;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class FilterExpression implements JsonLogicExpression {
	public static final FilterExpression INSTANCE = new FilterExpression();

	private FilterExpression() {
		// Use INSTANCE instead.
	}

	@Override
	public String key() {
		return "filter";
	}

	@Override
	public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath)
			throws JsonLogicEvaluationException {
		if (arguments.size() != 2) {
			throw new JsonLogicEvaluationException("filter expects exactly 2 arguments", jsonPath);
		}

		final Object maybeArray = evaluator.evaluate(arguments.get(0), data, jsonPath + "[0]");

		if (!ArrayLike.isArray(maybeArray)) {
			throw new JsonLogicEvaluationException("first argument to filter must be a valid array", jsonPath + "[0]");
		}

		final List<Object> result = new ArrayList<>();

		for (final Object item :ArrayLike.asList(maybeArray)) {
			if(JsonLogic.truthy(evaluator.evaluate(arguments.get(1), item, jsonPath + "[1]"))) {
				result.add(item);
			}
		}

		return result;
	}
}
