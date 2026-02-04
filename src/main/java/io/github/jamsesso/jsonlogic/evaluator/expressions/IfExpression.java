package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;

public class IfExpression implements JsonLogicExpression {
	public static final IfExpression IF = new IfExpression("if");
	public static final IfExpression TERNARY = new IfExpression("?:");

	private final String operator;

	private IfExpression(final String operator) {
		this.operator = operator;
	}

	@Override
	public String key() {
		return operator;
	}

	@Override
	public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data,
			final String jsonPath)
					throws JsonLogicEvaluationException {
		if (arguments.size() < 1) {
			return null;
		}

		// If there is only a single argument, simply evaluate & return that argument.
		if (arguments.size() == 1) {
			return evaluator.evaluate(arguments.get(0), data, jsonPath + "[0]");
		}

		// If there is 2 arguments, only evaluate the second argument if the first argument is truthy.
		if (arguments.size() == 2) {
			return JsonLogic.truthy(evaluator.evaluate(arguments.get(0), data, jsonPath + "[0]"))
					? evaluator.evaluate(arguments.get(1), data, jsonPath + "[1]")
							: null;
		}

		for (var i = 0; i < arguments.size() - 1; i += 2) {
			final var condition = arguments.get(i);
			final var resultIfTrue = arguments.get(i + 1);

			if (JsonLogic.truthy(evaluator.evaluate(condition, data, String.format("%s[%d]", jsonPath, i)))) {
				return evaluator.evaluate(resultIfTrue, data, String.format("%s[%d]", jsonPath, i + 1));
			}
		}

		if ((arguments.size() & 1) == 0) {
			return null;
		}

		return evaluator.evaluate(arguments.get(arguments.size() - 1), data, String.format("%s[%d]", jsonPath, arguments.size() - 1));
	}
}
