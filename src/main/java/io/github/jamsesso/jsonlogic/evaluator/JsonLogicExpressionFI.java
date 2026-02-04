package io.github.jamsesso.jsonlogic.evaluator;

import java.util.List;

public interface JsonLogicExpressionFI { Object evaluate(JsonLogicEvaluator evaluator, List<?> arguments, String jsonPath) throws JsonLogicEvaluationException; }
