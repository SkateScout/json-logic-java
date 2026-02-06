package io.github.jamsesso.jsonlogic.ast;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class JsonLogicParser {
	// Utility class has no public constructor.
	private JsonLogicParser() { }

	public static Object parse(final String jsonText) throws JsonLogicParseException {
		final var json = JSON.parse(jsonText);
		try { return parse(json, "$"); } catch (final Exception e) { throw new JsonLogicParseException(e, "$"); }
	}

	public static Object parse(final Object raw, final String jsonPath_) throws JsonLogicParseException {
		final var todo   = new LinkedList<Deferred>();
		final var result = new Object[1];
		todo.add(new Deferred(raw, jsonPath_, result, 0));
		do {
			final var cur      = todo.remove();
			final var jsonPath = cur.jsonPath();
			final var plain    = JSON.plain(cur.raw());
			switch(plain) {
			case null                  -> cur.accept(null);
			case final Number        t -> cur.accept(t   );
			case final String        t -> cur.accept(t   );
			case final Boolean       t -> cur.accept(t   );
			case final List<?>       t -> cur.accept(t   );
			case final Map<?,?>      t -> {
				if (t.size() != 1) throw new JsonLogicParseException("objects must have exactly 1 key defined, found " + t.size(), jsonPath);
				final var e       = t.entrySet().iterator().next();
				final var key     = e.getKey().toString().toLowerCase();
				final var args    = JSON.plain(e.getValue());
				final var argsMax = "var".equals(key) ? 2 : Integer.MAX_VALUE;
				final var argsUse = Math.min(args instanceof final List l ? l.size() : 1 , argsMax);
				final var argRet  = new Object[argsUse];
				if(args instanceof final List<?> l) {	// Always coerce single-argument operations into a List with a single element.
					var idx=0;
					for (final var element : l) { todo.add(new Deferred(element, jsonPath+" ["+idx+"]", argRet, idx)); idx++;  }
				} else                            todo.add(new Deferred(args   , jsonPath+" [0]"      , argRet, 0  ));

				if ("var".equals(key)) cur.accept(new JsonLogicVariable(argRet));					    // Special case for variable handling
				else                   cur.accept(new JsonLogicOperation(key, Arrays.asList(argRet)));  // Handle regular operations
			}
			default -> {
				if(!plain.getClass().isPrimitive()) throw new IllegalStateException("parse({"+plain.getClass().getCanonicalName()+"})");
				cur.accept(plain);
			}
			}
		} while(!todo.isEmpty());
		return result[0];
	}
}