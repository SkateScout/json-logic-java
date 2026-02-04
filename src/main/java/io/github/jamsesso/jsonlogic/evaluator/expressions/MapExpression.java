package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class MapExpression {
	/** Use INSTANCE instead. */
	private MapExpression() { }

	public static Object map(final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2)  throw new JsonLogicEvaluationException("map expects exactly 2 arguments", jsonPath);
		final var maybeArray = evaluator.evaluate(arguments.get(0), jsonPath + "[0]");
		if (!ArrayLike.isList(maybeArray)) return Collections.emptyList();
		final var list = ArrayLike.asList(maybeArray);
		final var ret = new Object[list.size()];
		var index = 0;
		for (final Object item : list) ret[index++] = evaluator.scoped(item).evaluate(arguments.get(1), jsonPath + "[1]");
		return Arrays.asList(ret);

	}
}