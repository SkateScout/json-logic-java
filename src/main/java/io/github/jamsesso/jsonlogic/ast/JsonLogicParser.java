package io.github.jamsesso.jsonlogic.ast;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.github.jamsesso.jsonlogic.NullableDeque;
import io.github.jamsesso.jsonlogic.PathSegment;

public final class JsonLogicParser {
	// Utility class has no public constructor.
	private JsonLogicParser() { }

	public static Object parse(final String jsonText) throws JsonLogicParseException {
		try { return parse(JSON.parse(jsonText), PathSegment.ROOT); } catch (final Exception e) { throw new JsonLogicParseException(e, PathSegment.ROOT); }
	}

	public static Object parse(final Object raw, final PathSegment jsonPath_) throws JsonLogicParseException {
		final var TASK_NODE   = 0;
		final var TASK_REDUCE = 1;
		final var todoRaw = new NullableDeque<>(512);
		final var values  = new NullableDeque<>(512);
		var todoArgCount = new int[256];
		var todoTaskType = new int[256];
		var intPtr  = -1;
		var taskPtr = -1;
		todoRaw.push(raw);
		todoRaw.push(jsonPath_);
		todoTaskType[++taskPtr] = TASK_NODE;
		while (taskPtr >= 0) {
			final var currentTask = todoTaskType[taskPtr--];
			switch (currentTask) {
			case TASK_REDUCE -> {
				final var size   = todoArgCount[intPtr--];
				final var key  = (String) todoRaw.pop();
				final var args = new Object[size];
				for (var i = size - 1; i >= 0; i--) args[i] = values.pop();
				if ("var".equals(key)) values.push(new JsonLogicVariable(args));
				else values.push(new JsonLogicOperation(key, Arrays.asList(args)));
			}
			case TASK_NODE -> {
				final var jsonPath = (PathSegment) todoRaw.pop();
				final var plain    = JSON.plain(todoRaw.pop());
				switch (plain) {
				case null -> values.push(null);
				case final Number    n -> values.push(n);
				case final String    s -> values.push(s);
				case final Boolean   b -> values.push(b);
				case final List<?>   l -> values.push(l);
				case final Map<?, ?> t -> {
					if (t.size() != 1) throw new JsonLogicParseException("objects must have single key defined, found " + t.size(), jsonPath);
					final var entry = t.entrySet().iterator().next();
					final var key = entry.getKey().toString().toLowerCase();
					final var rawArgs = JSON.plain(entry.getValue());
					final var argsMax  = "var".equals(key) ? 2 : Integer.MAX_VALUE;
					final var argsSize = (rawArgs instanceof final List<?> l) ? l.size() : 1;
					final var argsUse  = Math.min(argsSize, argsMax);
					if (taskPtr + argsUse + 1 >= todoTaskType.length) {
						final var newSize = todoTaskType.length * 2 + argsUse;
						todoTaskType = java.util.Arrays.copyOf(todoTaskType, newSize);
						todoArgCount = java.util.Arrays.copyOf(todoArgCount, newSize);
					}
					final var opPath = jsonPath.sub(key);
					todoRaw.push(key);
					todoArgCount[++intPtr ] = argsUse;
					todoTaskType[++taskPtr] = TASK_REDUCE;
					if (rawArgs instanceof final List<?> l) {
						for (var i = argsUse - 1; i >= 0; i--) {
							todoRaw.push(l.get(i));
							todoRaw.push(opPath.sub(i));
							todoTaskType[++taskPtr] = TASK_NODE;
						}
					} else {
						todoRaw.push(rawArgs);
						todoRaw.push(jsonPath.sub(0));
						todoTaskType[++taskPtr] = TASK_NODE;
					}
				}
				default -> { if (!plain.getClass().isPrimitive()) throw new IllegalStateException("Unexpected type: " + plain.getClass()); values.push(plain); }
				}
			}
			}
		}
		return values.pop();
	}
}