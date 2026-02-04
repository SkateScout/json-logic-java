package io.github.jamsesso.jsonlogic;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.github.jamsesso.jsonlogic.ast.JsonLogicOperation;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpressionFI;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public record Reduce<T>(Reducer<T> reducer, Getter<T> get, T init, int maxArguments, int minArguments) implements JsonLogicExpressionFI {
	public static interface Reducer<T> extends BiFunction<T, T, T> { };
	public static interface Getter <T> extends Function<Object,T> { }

	private static Getter<Double>  NUMERIC = value -> {
		if (value instanceof String) try { return Double.parseDouble((String) value); } catch (NumberFormatException e) { return null; }
		if (value instanceof Number t) return t.doubleValue();
		return null;
	};

	private static Getter<Double>  AP_NUMERIC = value -> NUMERIC.apply(getSingle(value));
	private static Reducer<Double> R_MINUS = (a, b) -> a - b;

	public static Map<String, Reduce<Double>> FUNCTIONS = Map.of(
			"-"  , new Reduce<>(R_MINUS        , AP_NUMERIC, null, 0, 1),
			"+"  , new Reduce<>(Double::sum    , AP_NUMERIC, null, 0, 1),
			"*"  , new Reduce<>((a, b) -> a * b, AP_NUMERIC, 1.  , 0, 1),
			"/"  , new Reduce<>((a, b) -> a / b,    NUMERIC, null, 2, 2),
			"%"  , new Reduce<>((a, b) -> a % b,    NUMERIC, null, 2, 2),
			"min", new Reduce<>(Math::min      ,    NUMERIC, null, 0, 1),
			"max", new Reduce<>(Math::max      ,    NUMERIC, null, 0, 1)
			);

	private static Object getSingle(Object value) {
		while (ArrayLike.isList(value)) {
			var array = ArrayLike.asList(value);
			if (array.isEmpty()) break;
			value = array.get(0);
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	@Override public T evaluate(final JsonLogicEvaluator evaluator, final List<?> args, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (args.isEmpty()) return null;
		var size = args.size();
		if(maxArguments > 0) size = Math.min(size, maxArguments);
		if(size < minArguments) return null;
		T accumulator = init;
		var i = 0;
		if(null == init) {
			var cur = evaluator.evaluate(args.get(i++), data, jsonPath+"["+(i-1)+"]");
			if(null == (accumulator = get.apply(cur))) return null;
			if(R_MINUS == reducer && size == 1) return (T)Double.valueOf(-((Number)cur).doubleValue());
		}
		for (; i < size; i++) {
			var value = evaluator.evaluate(args.get(i), data, jsonPath+"["+(i)+"]");
			if(value instanceof JsonLogicOperation) throw new IllegalStateException();
			T cur = get.apply(value);
			if(null == cur) return null;
			accumulator = reducer.apply(accumulator, cur);
		}
		return accumulator;
	}
}