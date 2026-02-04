package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;

public class SubstringExpression implements JsonLogicExpression {
	public static final SubstringExpression INSTANCE = new SubstringExpression();

	private SubstringExpression() {
		// Use INSTANCE instead.
	}

	@Override public String key() { return "substr"; }

	@Override public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
		if (args.size() < 2 || args.size() > 3) throw new JsonLogicEvaluationException("substr expects 2 or 3 arguments", jsonPath);
		if (!(evaluator.asDouble(args.get(1), jsonPath) instanceof final Double arg1)) throw new JsonLogicEvaluationException("second argument to substr must be a number", jsonPath + "[1]");
		final var value = evaluator.evaluate(args.get(0), jsonPath).toString();
		final var len = value.length();
		if (args.size() == 2) {
			var startIndex = arg1.intValue();
			final var endIndex = len;
			if (startIndex < 0) startIndex = endIndex + startIndex;
			if (startIndex < 0) return "";
			return value.substring(startIndex, endIndex);
		}
		if (!(evaluator.asDouble(args.get(2), jsonPath) instanceof final Double arg2)) throw new JsonLogicEvaluationException("third argument to substr must be an integer", jsonPath + "[2]");
		var startIndex = arg1.intValue();
		if (startIndex < 0) startIndex = value.length() + startIndex;
		var endIndex = arg2.intValue();
		if (endIndex < 0) endIndex = len + endIndex; else endIndex += startIndex;
		if (startIndex > endIndex || endIndex > value.length()) return "";
		return value.substring(startIndex, endIndex);
	}
}