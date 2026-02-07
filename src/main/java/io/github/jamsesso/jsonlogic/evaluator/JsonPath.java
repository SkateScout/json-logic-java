package io.github.jamsesso.jsonlogic.evaluator;

import java.util.List;
import java.util.Map;

import io.github.jamsesso.jsonlogic.PathSegment;
import io.github.jamsesso.jsonlogic.ast.JSON;

public class JsonPath {
	private JsonPath()  { }
	/** Sentinel object to represent a missing value (for internal use only). */
	public static final Object MISSING = new Object();

	public static Object evaluate(final Object keyParam, PathSegment jsonPath, final Object data) throws JsonLogicEvaluationException {
		jsonPath = jsonPath.sub("var");
		if (keyParam  == null) return data;
		if (data == null) return MISSING;
		if (keyParam instanceof final Number idx) {
			final var index = idx.intValue();
			if(index < 0 || !JSON.isList(data)) return MISSING;
			return(JSON.asList(data) instanceof final List l && index < l.size() ? l.get(index) : MISSING);
		}
		// Handle the case when the key is a string, potentially referencing an infinitely-deep map: x.y.z
		if (keyParam instanceof final String name) {
			if (name.isEmpty()) return data;
			final var keys = name.split("\\.");
			var result = data;
			jsonPath = jsonPath.sub(0);
			for (final var key : keys) {
				if(result == null || result == MISSING) return result;
				result = JSON.plain(result);
				if (JSON.isList(result)) {
					final int index;
					try { index = Integer.parseInt(key); } catch (final NumberFormatException e) { throw new JsonLogicEvaluationException(e, jsonPath); }
					if((index < 0) || !(JSON.asList(result) instanceof final List l) || (index >= l.size())) return MISSING;
					result =  l.get(index);
				} else if (result instanceof final Map map) {
					if(!map.containsKey(key)) return MISSING;
					result = map.get(key);
				} else throw new JsonLogicEvaluationException("Variable type "+result.getClass().getCanonicalName()+" unsupported", jsonPath);

			}
			return result;
		}
		throw new JsonLogicEvaluationException("var first argument must be null, number, or string", jsonPath.sub(0));
	}
}