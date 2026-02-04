package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class ReduceExpression {
	// Use INSTANCE instead.
	private ReduceExpression() { }

	public static Object reduce(final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 3) throw new JsonLogicEvaluationException("reduce expects exactly 3 arguments", jsonPath);
		final var maybeArray  = evaluator.evaluate(arguments.get(0), jsonPath + "[0]");
		final var accumulator = evaluator.evaluate(arguments.get(2), jsonPath + "[2]");
		if (!ArrayLike.isList(maybeArray)) return accumulator;
		final Map<String, Object> context = new HashMap<>();
		context.put("accumulator", accumulator);
		for (final var item : ArrayLike.asList(maybeArray)) {
			context.put("current", item);
			context.put("accumulator", evaluator.scoped(context).evaluate(arguments.get(1), jsonPath + "[1]"));
		}
		return context.get("accumulator");
	}
}