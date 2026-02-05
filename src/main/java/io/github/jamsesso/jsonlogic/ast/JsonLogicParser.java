package io.github.jamsesso.jsonlogic.ast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class JsonLogicParser {
	// Utility class has no public constructor.
	private JsonLogicParser() { }

	public static Object parse(final String jsonText) throws JsonLogicParseException {
		final var json = JSON.parse(jsonText);
		try { return parse(json, "$"); } catch (final Exception e) { throw new JsonLogicParseException(e, "$"); }
	}

	private static JsonLogicNode parseMap(final Map<Object,Object> map, final String jsonPath) throws JsonLogicParseException {
		// Handle objects & variables
		if (map.size() != 1) throw new JsonLogicParseException("objects must have exactly 1 key defined, found " + map.size(), jsonPath);
		final var key = map.keySet().iterator().next().toString().toLowerCase();
		final var argumentNode = parse(map.get(key), String.format("%s.%s", jsonPath, key));

		final List<Object> arguments;
		// Always coerce single-argument operations into a JsonLogicArray with a single element.
		if(argumentNode instanceof final List<?> t) {
			final var argSize = t.size();
			final var argRet  = new Object[argSize];
			var idx=0;
			for (final var element : t) { argRet[idx] = parse(element, jsonPath+" ["+idx+"]"); idx++; }
			arguments = Arrays.asList(argRet);
		}
		else arguments = Collections.singletonList(parse(argumentNode, jsonPath+" ["+0+"]"));

		// Special case for variable handling
		if ("var".equals(key)) {
			final var  defaultValue =    arguments.size() > 1 ?        arguments.get(1) : null;
			return new JsonLogicVariable(arguments.size() < 1 ? null : arguments.get(0), defaultValue);
		}
		// Handle regular operations
		return new JsonLogicOperation(key, arguments);
	}

	public static Object parse(final Object raw, final String jsonPath) throws JsonLogicParseException {
		// Handle primitives
		final var plain = JSON.plain(raw);
		if(plain == null                              ) return null;
		if(plain instanceof final Number             t) return t;
		if(plain instanceof final String             t) return t;
		if(plain instanceof final Boolean            t) return t;
		if(plain instanceof final List<?>            t) return t;
		if(plain instanceof final JsonLogicOperation t) return t;
		if(plain instanceof final Map                t) return parseMap(t, jsonPath);
		if(plain.getClass().isPrimitive()             ) return plain;
		throw new IllegalStateException("parse({"+plain.getClass().getCanonicalName()+"})");
	}
}