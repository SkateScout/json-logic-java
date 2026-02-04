package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class InExpression implements PreEvaluatedArgumentsExpression {
	public static final InExpression INSTANCE = new InExpression();

	private InExpression() {
		// Use INSTANCE instead.
	}

	@Override public String key() { return "in"; }

	@Override
	public Object evaluate(final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() < 2) return false;
		// Handle string in (substring)
		if (arguments.get(1) instanceof String) {
			if (arguments.get(0) == null) return false;
			return ((String) arguments.get(1)).contains(arguments.get(0).toString());
		}

		if (ArrayLike.isArray(arguments.get(1))) return ArrayLike.asList(arguments.get(1)).contains(arguments.get(0));
		return false;
	}
}
