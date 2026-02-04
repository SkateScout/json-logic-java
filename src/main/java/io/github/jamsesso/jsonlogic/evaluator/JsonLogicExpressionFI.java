package io.github.jamsesso.jsonlogic.evaluator;

import java.util.List;

public interface JsonLogicExpressionFI {
	Object evaluate(JsonLogicEvaluator evaluator, List<?> arguments, Object data, String jsonPath) throws JsonLogicEvaluationException;
}
