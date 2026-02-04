package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;

public class StrictEqualityExpression implements PreEvaluatedArgumentsExpression {
	public static final StrictEqualityExpression INSTANCE = new StrictEqualityExpression();

	private StrictEqualityExpression() {
		// Only one instance can be constructed. Use StrictEqualityExpression.INSTANCE
	}

	@Override
	public String key() {
		return "===";
	}

	@Override
	public Object evaluate(final List arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2) {
			throw new JsonLogicEvaluationException("equality expressions expect exactly 2 arguments", jsonPath);
		}

		final var left = arguments.get(0);
		final var right = arguments.get(1);

		if (left instanceof Number && right instanceof Number) {
			return ((Number) left).doubleValue() == ((Number) right).doubleValue();
		}

		if (left == right) {
			return true;
		}

		return left != null && left.equals(right);
	}
}
