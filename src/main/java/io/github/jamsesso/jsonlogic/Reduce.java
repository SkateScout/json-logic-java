package io.github.jamsesso.jsonlogic;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpressionFI;

public record Reduce<T>(Reducer<T> reducer, int maxArguments, int minArguments) implements JsonLogicExpressionFI {
	public static interface Reducer<T> extends BiFunction<T, T, T> { }
	public static interface Getter <T> extends Function<Object,T> { }

	private static Reducer<Double> R_MINUS = (a, b) -> a - b;

	public static Map<String, Reduce<Double>> FUNCTIONS = Map.of(
			"-"  , new Reduce<>(R_MINUS        , 0, 1),
			"+"  , new Reduce<>(Double::sum    , 0, 1),
			"*"  , new Reduce<>((a, b) -> a * b, 0, 1),
			"/"  , new Reduce<>((a, b) -> a / b, 2, 2),
			"%"  , new Reduce<>((a, b) -> a % b, 2, 2),
			"min", new Reduce<>(Math::min      , 0, 1),
			"max", new Reduce<>(Math::max      , 0, 1)
			);

	@SuppressWarnings("unchecked")
	@Override public T evaluate(final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
		if (args.isEmpty()) return null;
		var size = args.size();
		if(maxArguments > 0) size = Math.min(size, maxArguments);
		if(size < minArguments) return null;
		T accumulator = null;
		if(null == (accumulator = (T)evaluator.asDouble(args.get(0), jsonPath+"[0]"))) return null;
		if(R_MINUS == reducer && size == 1) return (T)Double.valueOf(-((Number)accumulator).doubleValue());
		for (var i=1; i < size; i++) {
			final var cur = evaluator.asDouble(args.get(i), jsonPath+"["+(i)+"]");
			if(null == cur) return null;
			accumulator = reducer.apply(accumulator, (T)cur);
		}
		return accumulator;
	}
}