package io.github.jamsesso.jsonlogic.evaluator;

import java.util.List;

import io.github.jamsesso.jsonlogic.PathSegment;

public interface JsonLogicExpressionFI { Object evaluate(JsonLogicEvaluator evaluator, List<?> arguments, PathSegment jsonPath) throws JsonLogicEvaluationException; }
