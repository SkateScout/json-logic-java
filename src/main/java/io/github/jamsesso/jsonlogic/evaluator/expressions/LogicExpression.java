package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;

public class LogicExpression implements JsonLogicExpression {
	public static final LogicExpression AND = new LogicExpression(true);
	public static final LogicExpression OR = new LogicExpression(false);

	private final boolean isAnd;

	private LogicExpression(final boolean isAnd) {
		this.isAnd = isAnd;
	}

	@Override
	public String key() {
		return isAnd ? "and" : "or";
	}

	@Override
	public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath)
			throws JsonLogicEvaluationException {
		if (arguments.size() < 1) {
			throw new JsonLogicEvaluationException(key() + " operator expects at least 1 argument", jsonPath);
		}

		Object result = null;

		var index = 0;
		for (final var element : arguments) {
			result = evaluator.evaluate(element, data, String.format("%s[%d]", jsonPath, index++));

			if ((isAnd && !JsonLogic.truthy(result)) || (!isAnd && JsonLogic.truthy(result))) {
				return result;
			}
		}

		return result;
	}
}
