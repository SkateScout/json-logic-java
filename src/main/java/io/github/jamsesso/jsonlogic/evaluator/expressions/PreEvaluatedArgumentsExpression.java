package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.PathSegment;
import io.github.jamsesso.jsonlogic.ast.JSON;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;

public interface PreEvaluatedArgumentsExpression extends JsonLogicExpression {
	Object evaluate(List<?> arguments, PathSegment jsonPath) throws JsonLogicEvaluationException;

	@Override default Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment jsonPath) throws JsonLogicEvaluationException {
		var values = evaluator.evaluate(arguments, jsonPath);
		if (values.size() == 1 && JSON.isList(values.get(0))) values = JSON.asList(values.get(0));
		return evaluate(values, jsonPath);
	}
}