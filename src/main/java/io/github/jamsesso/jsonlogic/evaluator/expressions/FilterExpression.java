package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.ArrayList;
import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class FilterExpression {
	// Use INSTANCE instead.
	private FilterExpression() { }

	public static Object filter(final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2) throw new JsonLogicEvaluationException("filter expects exactly 2 arguments", jsonPath);

		final var maybeArray = evaluator.evaluate(arguments.get(0), jsonPath + "[0]");

		if (!ArrayLike.isList(maybeArray)) throw new JsonLogicEvaluationException("first argument to filter must be a valid array", jsonPath + "[0]");

		final List<Object> result = new ArrayList<>();
		final var filter = arguments.get(1);
		for (final Object item : ArrayLike.asList(maybeArray)) if(evaluator.scoped(item).asBoolean(filter, jsonPath + "[1]")) result.add(item);
		return result;
	}
}
