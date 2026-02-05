package io.github.jamsesso.jsonlogic;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpressionFI;

public record Reduce(BiFunction<Double, Double, Double> reducer, int maxArguments, int minArguments) implements JsonLogicExpressionFI {
	private static BiFunction<Double, Double, Double> R_MINUS = (a, b) -> a - b;

	public static Map<String, Reduce> FUNCTIONS = Map.of(
			"-"  , new Reduce(R_MINUS        , 0, 1),
			"+"  , new Reduce(Double::sum    , 0, 1),
			"*"  , new Reduce((a, b) -> a * b, 0, 1),
			"/"  , new Reduce((a, b) -> a / b, 2, 2),
			"%"  , new Reduce((a, b) -> a % b, 2, 2),
			"min", new Reduce(Math::min      , 0, 1),
			"max", new Reduce(Math::max      , 0, 1)
			);

	@Override public Double evaluate(final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
		if (args.isEmpty()) return null;
		var size = args.size();
		if(maxArguments > 0) size = Math.min(size, maxArguments);
		if(size < minArguments) return null;
		Double accumulator = null;
		for (var i=0; i < size; i++) {
			if(!(evaluator.asDouble(args.get(i), jsonPath+"["+i+"]") instanceof final Double cur)) return null;
			if(i > 0) { accumulator = reducer.apply(accumulator, cur); continue; }
			accumulator = cur;
			if(R_MINUS == reducer && size == 1) return -accumulator.doubleValue();
		}
		return accumulator;
	}
}