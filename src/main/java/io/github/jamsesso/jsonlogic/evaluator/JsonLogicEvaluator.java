package io.github.jamsesso.jsonlogic.evaluator;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.jamsesso.jsonlogic.INumeric;
import io.github.jamsesso.jsonlogic.NullableDeque;
import io.github.jamsesso.jsonlogic.PathSegment;
import io.github.jamsesso.jsonlogic.ast.JSON;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicOperation;
import io.github.jamsesso.jsonlogic.ast.JsonLogicVariable;

public record JsonLogicEvaluator(Map<String, JsonLogicExpressionFI> expressions, INumeric number, Object data) {
	public JsonLogicEvaluator { expressions = Collections.unmodifiableMap(expressions); }
	public JsonLogicEvaluator scoped(final Object scopeData) { return new JsonLogicEvaluator(expressions, number, scopeData); }

	// sealed interface Todo permits Value, Varable, Handler { }
	// record Value  (Object[] val                 , Deferred d) implements Todo { void run() { d.accept(val[0]); } }
	// record Varable(Object[] key, Object fallback, Deferred d) implements Todo { }
	// record Handler(JsonLogicExpressionFI h, Object[] result, Deferred d, PathSegment jsonPath) implements Todo {
	// 	void run(final JsonLogicEvaluator e) throws JsonLogicEvaluationException {
	// 		final var r = result[0];
	// 		final var args = (JSON.isList(r) ? JSON.asList(r) : Collections.singletonList(r));
	// 		d.accept(h.evaluate(e, args, jsonPath));
	// 	}
	// }

	static enum TASK { LIST, VAR, TASK }
	public Object evaluate(final Object logic, final PathSegment jsonPath_) throws JsonLogicEvaluationException {
		final var todo   = new NullableDeque<>(128);
		final var values = new NullableDeque<>(128);
		todo.push(TASK.TASK);
		todo.push(jsonPath_);
		todo.push(logic);
		while (!todo.isEmpty()) {
			final var next = todo.pop();
			switch (next) {
			case null                  -> values.push(null);
			case final Boolean       t -> values.push(t);
			case final String        t -> values.push(t);
			case final PathSegment   t -> values.push(t);
			case final Number        t -> values.push(t);
			case final List<?>       t -> values.push(t);
			case final JsonLogicNode t -> values.push(t);
			case final TASK   t -> {
				switch(t) {
				case LIST -> {
					final var size         = (Integer)values.pop();
					final var results = new Object[size];
					for (var i = size - 1; i >= 0; i--) results[i] = values.pop();
					values.push(Arrays.asList(results));
				}
				case VAR -> {
					final var path         = (PathSegment)values.pop();
					final var key          = values.pop();
					final var defaultValue = values.pop();
					final var res          = JsonPath.evaluate(key, path, data);
					if(res == JsonPath.MISSING) values.push(defaultValue);
					else                        values.push(res);
				}
				case TASK -> {
					final var path = (PathSegment)values.pop();
					final var p0   = values.pop();
					switch (p0) {
					case null             -> { values.push(null); continue; }
					case final Number   _ -> { values.push(p0); continue; }
					case final String   _ -> { values.push(p0); continue; }
					case final Boolean  _ -> { values.push(p0); continue; }
					case final Map<?,?> _ -> { values.push(p0); continue; }
					case final List<?> args -> {
						final var size = args.size();
						todo.push(TASK.LIST);
						todo.push(size);
						for (var i = size - 1; i >= 0; i--) {
							todo.push(TASK.TASK);
							todo.push(path.sub(i));
							todo.push(args.get(i));
						}
					}
					case final JsonLogicOperation op -> {
						final var handler = expressions.get(op.operator());
						if (handler == null) throw new JsonLogicEvaluationException("Undefined: " + op.operator(), path);
						final var args = op.arguments();
						final var result = handler.evaluate(this, args == null ? Collections.EMPTY_LIST : args, path.sub(op.operator()));
						// System.out.println("OP["+op.operator()+"]("+args+")="+result);
						values.push(result);
					}
					case final JsonLogicVariable v -> {
						todo.push(TASK.VAR);
						todo.push(path);
						todo.push(TASK.TASK);
						todo.push(path.sub("key"));
						todo.push(v.key         ());
						todo.push(TASK.TASK);
						todo.push(path.sub("def"));
						todo.push(v.defaultValue());
					}
					default -> throw new IllegalStateException("Unexpected type: " + p0.getClass());
					}

				}
				}
			}
			default -> throw new IllegalStateException("Unexpected stack element: " + next);
			}
		}
		final var ret = values.pop();
		return (JsonPath.MISSING==ret ? null : ret);
	}

	@SuppressWarnings("unchecked")
	public List<Object> evaluate(final List<?> t, final PathSegment jsonPath) throws JsonLogicEvaluationException {
		return (List<Object>)evaluate((Object)t, jsonPath);
	}

	public Number asNumber(final Object p0, final PathSegment jsonPath) throws JsonLogicEvaluationException {
		final var value = evaluate(p0, jsonPath);
		if (value instanceof final String t) try { return Double.parseDouble(t); } catch (final NumberFormatException e) { return null; }
		if (value instanceof final Number t) return t;
		if (JSON.isList(value) && JSON.asList(value) instanceof final List<?> l && !l.isEmpty()) return asNumber(l.get(0), jsonPath);
		return null;
	}

	public Double asDouble(final Object p0, final PathSegment jsonPath) throws JsonLogicEvaluationException {
		final var value = evaluate(p0, jsonPath);
		if (value instanceof final String t) try { return Double.parseDouble(t); } catch (final NumberFormatException e) { return null; }
		if (value instanceof final Number t) return t.doubleValue();
		if (JSON.isList(value) && JSON.asList(value) instanceof final List<?> l && !l.isEmpty()) return asDouble(l.get(0), jsonPath);
		return null;
	}

	public static boolean asBoolean(final Object value) {
		if (value == null) return false;
		if (value instanceof final String     s) return !s.isEmpty();
		if (value instanceof final Boolean    v) return v;
		if (value instanceof final Number     n) {
			if (n instanceof final Double     d) return(!d.isNaN() && (d.isInfinite() || d.doubleValue() != 0.0));
			if (n instanceof final Float      f) return(!f.isNaN() && (f.isInfinite() || f.floatValue () != 0.0));
			return n.doubleValue() != 0.0;
		}
		if (value instanceof final Collection c) return !c.isEmpty();
		return(!value.getClass().isArray() || Array.getLength(value) > 0);
	}

	public boolean asBoolean(final Object p0, final PathSegment jsonPath) throws JsonLogicEvaluationException { return asBoolean(evaluate(p0, jsonPath)); }
}