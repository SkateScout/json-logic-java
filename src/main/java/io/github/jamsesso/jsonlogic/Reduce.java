package io.github.jamsesso.jsonlogic;

import java.util.List;
import java.util.function.BiFunction;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpressionFI;

public record Reduce(BiFunction<Number, Number, Number> reducer, int maxArguments, int minArguments, boolean isMinus) implements JsonLogicExpressionFI {
	@Override public Number evaluate(final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
		if (args.isEmpty()) return null;
		var size = args.size();
		if(maxArguments > 0) size = Math.min(size, maxArguments);
		if(size < minArguments) return null;
		Number accumulator = null;
		for (var i=0; i < size; i++) {
			if(!(evaluator.asNumber(args.get(i), jsonPath+"["+i+"]") instanceof final Number cur)) return null;
			if(i > 0) { accumulator = reducer.apply(accumulator, cur); continue; }
			accumulator = cur;
			if(isMinus && size == 1) return evaluator.number().MINUS(0, accumulator);
		}
		return accumulator;
	}
}