package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public interface PreEvaluatedArgumentsExpression extends JsonLogicExpression {
	Object evaluate(List<?> arguments, String jsonPath) throws JsonLogicEvaluationException;

	@Override default Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		List<Object> values = evaluator.evaluate(arguments, jsonPath);
		if (values.size() == 1 && ArrayLike.isList(values.get(0))) values = ArrayLike.asList(values.get(0));
		return evaluate(values, jsonPath);
	}
}
