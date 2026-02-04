package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import io.github.jamsesso.jsonlogic.utils.MapLike;

public class MissingExpression implements PreEvaluatedArgumentsExpression {
	public static final MissingExpression ALL = new MissingExpression(false);
	public static final MissingExpression SOME = new MissingExpression(true);

	private final boolean isSome;

	private MissingExpression(final boolean isSome) {
		this.isSome = isSome;
	}

	@Override
	public String key() {
		return isSome ? "missing_some" : "missing";
	}

	@Override
	public Object evaluate(final List arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (isSome && (arguments.size() < 2 || !ArrayLike.isArray(arguments.get(1)) || !(arguments.get(0) instanceof Double))) {
			throw new JsonLogicEvaluationException("missing_some expects first argument to be an integer and the second " +
					"argument to be an array", jsonPath);
		}

		if (!MapLike.isMap(data)) {
			if (isSome) {
				if (((Double) arguments.get(0)).intValue() <= 0) {
					return Collections.EMPTY_LIST;
				}
				return arguments.get(1);
			}
			return arguments;
		}

		final Map map = MapLike.asMap(data);
		final var options = isSome ?ArrayLike.asList(arguments.get(1)) : arguments;
		final var providedKeys = getFlatKeys(map);
		final Set requiredKeys = new LinkedHashSet(options);

		requiredKeys.removeAll(providedKeys); // Keys that I need but do not have

		if (isSome && options.size() - requiredKeys.size() >= ((Double) arguments.get(0)).intValue()) {
			return Collections.EMPTY_LIST;
		}

		return new ArrayList<>(requiredKeys);
	}

	/**
	 * Given a map structure such as:
	 * {a: {b: 1}, c: 2}
	 *
	 * This method will return the following set:
	 * ["a.b", "c"]
	 */
	private static Set getFlatKeys(final Map map) {
		return getFlatKeys(map, "");
	}

	private static Set getFlatKeys(final Map map, final String prefix) {
		final var keys = new LinkedHashSet<>();

		for (final Object pair : map.entrySet()) {
			final var entry = (Map.Entry) pair;

			if (MapLike.isMap(entry.getValue())) {
				keys.addAll(getFlatKeys(MapLike.asMap(entry.getValue()), prefix + entry.getKey() + "."));
			}
			else {
				keys.add(prefix + entry.getKey());
			}
		}

		return keys;
	}
}
