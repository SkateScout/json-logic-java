package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.ast.JSON;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;

public class EqualityExpression {
	// Only one instance can be constructed. Use EqualityExpression.INSTANCE
	private EqualityExpression() { }

	public static boolean equalityValue(final Object left, final Object right) throws JsonLogicEvaluationException {
		// Use the loose equality matrix
		// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Equality_comparisons_and_sameness#Loose_equality_using
		if ((left ==         right) || (left == null && right == null)) return true;
		if (left == null || right == null) return false;
		// Check numeric loose equality
		if (left instanceof final Number  l && right instanceof final Number  r) return Double.valueOf(l.doubleValue()).equals(r.doubleValue());
		if (left instanceof final Number  l && right instanceof final String  r) return compareNumberToString(l, r);
		if (left instanceof final Number  l && right instanceof final Boolean r) return compareNumberToBoolean(l, r);
		// Check string loose equality
		if (left instanceof final String  l && right instanceof final String  r) return l.equals(r);
		if (left instanceof final String  l && right instanceof final Number  r) return compareNumberToString(r, l);
		if (left instanceof final String  l && right instanceof final Boolean r) return compareStringToBoolean(l, r);
		// Check boolean loose equality
		if (left instanceof final Boolean l && right instanceof final Boolean r) return r.booleanValue() == l.booleanValue();
		if (left instanceof final Boolean l && right instanceof final Number  r) return compareNumberToBoolean(r, l);
		if (left instanceof final Boolean l && right instanceof final String  r) return compareStringToBoolean(r, l);
		if (left instanceof final List<?> l && right instanceof final List<?> r) return JSON.equals(r, l);
		// Check non-truthy values
		return !JsonLogicEvaluator.asBoolean(left) && !JsonLogicEvaluator.asBoolean(right);
	}

	public static boolean equality(final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2) throw new JsonLogicEvaluationException("equality expressions expect exactly 2 arguments", jsonPath);
		final var left  = evaluator.evaluate(arguments.get(0), jsonPath);
		final var right = evaluator.evaluate(arguments.get(1), jsonPath);
		return equalityValue(left, right);
	}

	private static boolean compareNumberToString(final Number left, final String right) {
		try { return (right.trim().isEmpty()) ? 0 == left.doubleValue() : Double.parseDouble(right) == left.doubleValue(); }
		catch (final NumberFormatException e) { return false; }
	}

	private static boolean compareNumberToBoolean(final Number left, final Boolean right) { return (right ? left.doubleValue() == 1.0 : left.doubleValue() == 0.0); }
	private static boolean compareStringToBoolean(final String left, final Boolean right) { return JsonLogicEvaluator.asBoolean(left) == right; }
}