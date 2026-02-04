package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;
import java.util.function.BiFunction;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class MathExpression implements PreEvaluatedArgumentsExpression {
	public static final BiFunction<Double, Double, Double> R_ADD      = Double::sum    ;
	public static final BiFunction<Double, Double, Double> R_SUBTRACT = (a, b) -> a - b;
	public static final BiFunction<Double, Double, Double> R_MULTIPLY = (a, b) -> a * b;
	public static final BiFunction<Double, Double, Double> R_DIVIDE   = (a, b) -> a / b;
	public static final BiFunction<Double, Double, Double> R_MODULO   = (a, b) -> a % b;
	public static final BiFunction<Double, Double, Double> R_MIN      = Math::min      ;
	public static final BiFunction<Double, Double, Double> R_MAX      = Math::max      ;

	public static final MathExpression ADD      = new MathExpression("+"  , R_ADD     , 0);
	public static final MathExpression SUBTRACT = new MathExpression("-"  , R_SUBTRACT, 2);
	public static final MathExpression MULTIPLY = new MathExpression("*"  , R_MULTIPLY, 0);
	public static final MathExpression DIVIDE   = new MathExpression("/"  , R_DIVIDE  , 2);
	public static final MathExpression MODULO   = new MathExpression("%"  , R_MODULO  , 2);
	public static final MathExpression MIN      = new MathExpression("min", R_MIN     , 0);
	public static final MathExpression MAX      = new MathExpression("max", R_MAX     , 0);

	private final String key;
	private final BiFunction<Double, Double, Double> reducer;
	private final int maxArguments;

	public MathExpression(final String key, final BiFunction<Double, Double, Double> reducer) { this(key, reducer, 0); }

	public MathExpression(final String key, final BiFunction<Double, Double, Double> reducer, final int maxArguments) {
		this.key = key;
		this.reducer = reducer;
		this.maxArguments = maxArguments;
	}

	@Override public String key() { return key; }

	private static Number number(final boolean PM, Object value) {
		if (PM) while (ArrayLike.isArray(value)) {
			final var array =ArrayLike.asList(value);
			if (array.isEmpty()) break;
			value = array.get(0);
		}
		if (value instanceof final String t) try { return Double.parseDouble(t); } catch (final NumberFormatException e) { return null; }
		if (value instanceof final Number t)  return t;
		return null;
	}

	@Override
	public Object evaluate(final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.isEmpty()) return null;
		final var size = arguments.size();
		if (size == 1) {
			if (reducer==R_SUBTRACT) return -number(false, arguments.get(0)).doubleValue();
			if (reducer==R_DIVIDE  ) return null;
		}
		// Collect all of the arguments
		final var PM = R_MULTIPLY ==  reducer || R_ADD == reducer;
		// Reduce the values into a single result
		var accumulator = number(PM, arguments.get(0)).doubleValue();
		for (var i = 1; i < size && (i<maxArguments || maxArguments==0); i++) {
			final var value = number(PM, arguments.get(i));
			if(null == value) return null;
			accumulator = reducer.apply(accumulator, value.doubleValue());
		}
		return accumulator;
	}
}