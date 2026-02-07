package io.github.jamsesso.jsonlogic;

import java.util.List;
import java.util.function.BiFunction;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpressionFI;

public record Reduce(BiFunction<Number, Number, Number> reducer, int maxArguments, int minArguments, boolean isMinus) implements JsonLogicExpressionFI {
	public Reduce { if(maxArguments < 1) maxArguments = Integer.MAX_VALUE; }
	@Override public Number evaluate(final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
		if (args.isEmpty()) return null;

		// if(maxArguments > 0) size = Math.min(size, maxArguments);
		// if(size < minArguments) return null;
		Number accumulator = null;
		final var todo = new NullableDeque<>(3 * args.size());
		for(var i=args.size()-1;i>=0;i--) todo.push(args.get(i));
		var argIdx = 0;
		while(!todo.isEmpty()) {
			if(argIdx >= maxArguments) return null;
			var v = todo.pop();
			if(v instanceof final List l && argIdx == 0) {
				if(args.size() == 1) {
					for(var i=l.size()-1;i>=0;i--) todo.push(l.get(i));
					continue;
				}
				if(l.isEmpty()) return null;
				v = l.get(0);
			}
			if(v instanceof JsonLogicNode) { todo.push(evaluator.evaluate(v, jsonPath)); continue; }
			if(argIdx == 0 && v instanceof List) { todo.push(args); continue; }
			if(!(evaluator.asNumber(v, jsonPath+"["+argIdx+"]") instanceof final Number cur)) return null;
			accumulator = 0 == argIdx ? cur : reducer.apply(accumulator, cur);
			argIdx++;
			if(argIdx == maxArguments) return accumulator;
		}
		if(isMinus && argIdx == 1) return evaluator.number().MINUS(0, accumulator);
		if(argIdx < minArguments) return  null;
		return accumulator;
	}
}