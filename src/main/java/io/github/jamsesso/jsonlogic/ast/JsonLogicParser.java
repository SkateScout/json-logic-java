package io.github.jamsesso.jsonlogic.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import io.github.jamsesso.jsonlogic.utils.MapLike;

public final class JsonLogicParser {
	// private static final JsonParser PARSER = new JsonParser();

	private JsonLogicParser() {
		// Utility class has no public constructor.
	}

	public static JsonLogicNode parse(final String json) throws JsonLogicParseException {
		//  try {
		//    return parse(PARSER.parse(json));
		//  }
		//  catch (JsonSyntaxException e) {
		//    throw new JsonLogicParseException(e, "$");
		//  }
		throw new UnsupportedOperationException("TBD");
	}

	private static Object parse(final Object root) throws JsonLogicParseException { return parse(root, "$"); }
	private static Object parse(final Object root, final String jsonPath) throws JsonLogicParseException {
		// Handle null
		if(root == null) return null;
		if(root instanceof final String  t) return t;
		if(root instanceof final Number  t) return t;
		if(root instanceof final Boolean t) return t;
		final var rc = root.getClass();
		if(rc.isPrimitive()) return root;

		// Handle arrays
		if(ArrayLike.isArray(root)) {
			final var array = ArrayLike.asList(root);
			final List<Object> elements = new ArrayList<>(array.size());

			var index = 0;
			for (final var element : array) elements.add(parse(element, String.format("%s[%d]", jsonPath, index++)));
			return elements;
		}
		if(MapLike.isMap(root)) throw new JsonLogicParseException("not an map", jsonPath);
		// Handle objects & variables
		final var object = MapLike.asMap(root);



		if (object.size() != 1) throw new JsonLogicParseException("objects must have exactly 1 key defined, found " + object.size(), jsonPath);
		final var key = object.keySet().iterator().next().toString();

		// Always coerce single-argument operations into a JsonLogicArray with a single element.
		final var argumentNode = parse(object.get(key), String.format("%s.%s", jsonPath, key));
		final var arguments = ArrayLike.isArray(argumentNode) ? ArrayLike.asList(argumentNode) : Collections.singletonList(argumentNode);


		// Special case for variable handling
		if ("var".equals(key)) {
			final var defaultValue = arguments.size() > 1 ? arguments.get(1) : null;
			return new JsonLogicVariable(arguments.size() < 1 ? null : arguments.get(0), defaultValue);
		}

		// Handle regular operations
		return new JsonLogicOperation(key, arguments);
	}
}
