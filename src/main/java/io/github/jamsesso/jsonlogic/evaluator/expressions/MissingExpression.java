package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpressionFI;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import io.github.jamsesso.jsonlogic.utils.MapLike;

public record MissingExpression(boolean isSome) implements JsonLogicExpressionFI {
	@Override
	public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
		if (isSome && (args.size() < 2)) throw new JsonLogicEvaluationException("missing_some expects first argument to be an integer and the second argument to be an array", jsonPath);
		var values = evaluator.evaluate(args, jsonPath);
		if (values.size() == 1 && ArrayLike.isList(values.get(0))) values = ArrayLike.asList(values.get(0));
		final var arguments= evaluator.evaluate(values, jsonPath);

		Double someCnt = 0.;
		if(isSome && (!ArrayLike.isList(arguments.get(1)) || (null == (someCnt = evaluator.asDouble(args.get(0), jsonPath)))))
			throw new JsonLogicEvaluationException("missing_some expects first argument to be an integer and the second argument to be an array", jsonPath);


		if (!MapLike.isEligible(evaluator.data())) return (isSome ? (someCnt.intValue() <= 0 ? Collections.EMPTY_LIST : arguments.get(1)) : arguments);

		final var map          = MapLike.asMap(evaluator.data());
		final var options      = isSome ? ArrayLike.asList(arguments.get(1)) : arguments;
		final var providedKeys = getFlatKeys(map);
		final var requiredKeys = new LinkedHashSet<>(options);
		requiredKeys.removeAll(providedKeys); // Keys that I need but do not have
		if (isSome && options.size() - requiredKeys.size() >= someCnt.intValue()) return Collections.EMPTY_LIST;
		return Arrays.asList(requiredKeys.toArray());
	}

	/**
	 * Given a map structure such as:
	 * {a: {b: 1}, c: 2}
	 *
	 * This method will return the following set:
	 * ["a.b", "c"]
	 */
	private static Set<String> getFlatKeys(final Map<?,?> map) { return getFlatKeys(map, ""); }

	private static Set<String> getFlatKeys(final Map<?,?> map, final String prefix) {
		final var keys = new LinkedHashSet<String>();
		for (final var entry : map.entrySet())
			if (MapLike.isEligible(entry.getValue())) keys.addAll(getFlatKeys(MapLike.asMap(entry.getValue()), prefix + entry.getKey() + "."));
			else keys.add(prefix + entry.getKey());

		return keys;
	}
}