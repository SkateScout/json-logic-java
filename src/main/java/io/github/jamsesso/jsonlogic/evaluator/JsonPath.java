package io.github.jamsesso.jsonlogic.evaluator;

import java.util.Map;

import io.github.jamsesso.jsonlogic.ast.JSON;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class JsonPath {
	private JsonPath()  { }
	/** Sentinel object to represent a missing value (for internal use only). */
	public static final Object MISSING = new Object();

	private static Object byIndex(final int index, final Object data) {
		var list = ArrayLike.asList(data);
		return (list == null || index < 0 || index >= list.size() ? MISSING : list.get(index));
	}

	private static Object partial(final String key, final Object result, final String jsonPath) throws JsonLogicEvaluationException {
		final var data = JSON.plain(result);
		if(data == null) return MISSING;
		if (ArrayLike.isList(data)) {
			final int index; try { index = Integer.parseInt(key); } catch (final NumberFormatException e) { throw new JsonLogicEvaluationException(e, jsonPath); }
			return byIndex(index, data);
		}
		if (data instanceof final Map map) return (map.containsKey(key) ? map.get(key) : MISSING);
		throw new JsonLogicEvaluationException("Variable type "+data.getClass().getCanonicalName()+" unsupported", jsonPath);
	}

	public static Object evaluate(final Object key, String jsonPath, final Object data) throws JsonLogicEvaluationException {
		jsonPath = jsonPath + ".var";
		if (key == null) return data;
		if (key instanceof final Number idx) return (ArrayLike.isList(data) ? byIndex(idx.intValue(), data) : MISSING);
		// Handle the case when the key is a string, potentially referencing an infinitely-deep map: x.y.z
		if (key instanceof final String name) {
			if (name.isEmpty()) return data;
			final var keys = name.split("\\.");
			var result = data;
			for (final var subKey : keys) if (null == (result = partial(subKey, result, jsonPath + "[0]")) || MISSING == result) return result;
			return result;
		}
		throw new JsonLogicEvaluationException("var first argument must be null, number, or string", jsonPath + "[0]");
	}
}