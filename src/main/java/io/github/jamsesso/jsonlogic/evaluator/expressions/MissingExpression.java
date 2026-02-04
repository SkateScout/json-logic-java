package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import io.github.jamsesso.jsonlogic.utils.MapLike;

public class MissingExpression implements JsonLogicExpression {
	public static final MissingExpression ALL = new MissingExpression(false);
	public static final MissingExpression SOME = new MissingExpression(true);

	private final boolean isSome;

	private MissingExpression(final boolean isSome) {
		this.isSome = isSome;
	}

	@Override public String key() { return isSome ? "missing_some" : "missing"; }

	@Override
	public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> args, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (isSome && (args.size() < 2)) throw new JsonLogicEvaluationException("missing_some expects first argument to be an integer and the second argument to be an array", jsonPath);
		Double someCnt = 0.;
		List<Object> values = evaluator.evaluate(args, data, jsonPath);
		if (values.size() == 1 && ArrayLike.isList(values.get(0))) values = ArrayLike.asList(values.get(0));
		var arguments= evaluator.evaluate(values, data, jsonPath);

		if (isSome) {
			if(!ArrayLike.isList(arguments.get(1)))
				throw new JsonLogicEvaluationException("missing_some expects first argument to be an integer and the second argument to be an array", jsonPath);

			if(null == (someCnt = evaluator.asDouble(args.get(0), data, jsonPath)))
				throw new JsonLogicEvaluationException("missing_some expects first argument to be an integer and the second argument to be an array", jsonPath);
		}


		if (!MapLike.isEligible(data)) {
			if (isSome) {
				if (someCnt.intValue() <= 0) return Collections.EMPTY_LIST;
				return arguments.get(1);
			}
			return arguments;
		}

		var map = new MapLike(data);
		var options = isSome ? ArrayLike.asList(arguments.get(1)) : arguments;
		var providedKeys = getFlatKeys(map);
		var requiredKeys = new LinkedHashSet<>(options);
		requiredKeys.removeAll(providedKeys); // Keys that I need but do not have
		if (isSome && options.size() - requiredKeys.size() >= someCnt.intValue()) return Collections.EMPTY_LIST;
		return new ArrayList<>(requiredKeys);
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
		var keys = new LinkedHashSet<String>();
		for (var entry : map.entrySet())
			if (MapLike.isEligible(entry.getValue())) keys.addAll(getFlatKeys(new MapLike(entry.getValue()), prefix + entry.getKey() + "."));
			else keys.add(prefix + entry.getKey());

		return keys;
	}
}