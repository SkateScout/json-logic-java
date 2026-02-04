package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class MapExpression implements JsonLogicExpression {
	public static final MapExpression INSTANCE = new MapExpression();

	/** Use INSTANCE instead. */
	private MapExpression() { }

	@Override public String key() { return "map"; }

	@Override public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2)  throw new JsonLogicEvaluationException("map expects exactly 2 arguments", jsonPath);
		final var maybeArray = evaluator.evaluate(arguments.get(0), jsonPath + "[0]");
		if (!ArrayLike.isList(maybeArray)) return Collections.emptyList();
		final List<Object> result = new ArrayList<>();
		for (final Object item : ArrayLike.asList(maybeArray)) result.add(evaluator.scoped(item).evaluate(arguments.get(1), jsonPath + "[1]"));
		return result;
	}
}