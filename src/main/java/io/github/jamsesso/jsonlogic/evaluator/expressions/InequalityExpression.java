package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;

public class InequalityExpression implements JsonLogicExpression {
	public static final InequalityExpression INSTANCE = new InequalityExpression(EqualityExpression.INSTANCE);

	private final EqualityExpression delegate;

	private InequalityExpression(final EqualityExpression delegate) {
		this.delegate = delegate;
	}

	@Override
	public String key() {
		return "!=";
	}

	@Override
	public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath)
			throws JsonLogicEvaluationException {
		final var result = (boolean) delegate.evaluate(evaluator, arguments, data, jsonPath);

		return !result;
	}
}
