package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class ReduceExpression implements JsonLogicExpression {
	public static final ReduceExpression INSTANCE = new ReduceExpression();

	private ReduceExpression() {
		// Use INSTANCE instead.
	}

	@Override
	public String key() {
		return "reduce";
	}

	@Override
	public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath)
			throws JsonLogicEvaluationException {
		if (arguments.size() != 3) {
			throw new JsonLogicEvaluationException("reduce expects exactly 3 arguments", jsonPath);
		}

		Object maybeArray  = evaluator.evaluate(arguments.get(0), data, jsonPath + "[0]");
		Object accumulator = evaluator.evaluate(arguments.get(2), data, jsonPath + "[2]");

		if (!ArrayLike.isList(maybeArray)) {
			return accumulator;
		}

		Map<String, Object> context = new HashMap<>();
		context.put("accumulator", accumulator);

		for (var item : ArrayLike.asList(maybeArray)) {
			context.put("current", item);
			context.put("accumulator", evaluator.evaluate(arguments.get(1), context, jsonPath + "[1]"));
		}

		return context.get("accumulator");
	}
}
