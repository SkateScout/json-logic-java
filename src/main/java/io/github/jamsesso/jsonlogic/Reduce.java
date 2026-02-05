package io.github.jamsesso.jsonlogic;

import java.util.List;
import java.util.function.BiFunction;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpressionFI;

public record Reduce(BiFunction<Double, Double, Double> reducer, int maxArguments, int minArguments) implements JsonLogicExpressionFI {
	public static BiFunction<Double, Double, Double> R_MINUS = (a, b) -> a - b;
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