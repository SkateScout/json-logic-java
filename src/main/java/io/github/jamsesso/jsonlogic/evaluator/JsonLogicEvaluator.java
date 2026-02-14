package io.github.jamsesso.jsonlogic.evaluator;

import java.util.Arrays;
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

	static enum TASK {
		/** stack:= N [N]<TASK path, value> */ LIST,
		/** stack:= path key default        */ VAR ,
		/** stack:= path value              */ TASK
	}

	public Object evaluate(final Object logic, final PathSegment jsonPath) throws JsonLogicEvaluationException {
		final var todo   = new NullableDeque<>(128);
		final var values = new NullableDeque<>(128);
		todo.push(TASK.TASK);
		todo.push(jsonPath);
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
					final var size    = (Integer)values.pop();
					final var results = new Object[size];
					for (var i = size - 1; i >= 0; i--) results[i] = values.pop();
					values.push(Arrays.asList(results));
				}
				case VAR -> {
					final var path         = (PathSegment)values.pop();
					final var key          = values.pop();
					final var defaultValue = values.pop();
					var res          = JsonPath.evaluate(key, path, data);
					if(res == JsonPath.MISSING) res = defaultValue;
					if(res instanceof final Number n) res = n.doubleValue();
					values.push(res);
				}
				case TASK -> {
					final var path = (PathSegment)values.pop();
					final var p0   =              values.pop();
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
						values.push(handler.evaluate(this, args == null ? Collections.EMPTY_LIST : args, path.sub(op.operator())));
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
			default -> throw new IllegalStateException("Unexpected stack element: {"+next.getClass().getCanonicalName()+"}" + next);
			}
		}
		final var ret = values.pop();
		return (JsonPath.MISSING==ret ? null : ret);
	}

	@SuppressWarnings("unchecked")
	public List<Object> evaluate(final List<?> t, final PathSegment path) throws JsonLogicEvaluationException {
		return (List<Object>)evaluate((Object)t, path);
	}

	private static boolean evalNeeded(final Object v) {
		return switch(v) {
		case null           -> false;
		case final String  _-> false;
		case final Number  _-> false;
		case final Boolean _-> false;
		default             -> true;
		};
	}

	public Number  asNumber (final Object p0               , final PathSegment path) throws JsonLogicEvaluationException {
		final var value = evalNeeded(p0) ? evaluate(p0, path) : p0;
		if (value instanceof final Number t) return t;
		if (value instanceof final String t) try { return Double.parseDouble(t); } catch (final NumberFormatException e) { return null; }
		if (JSON.isList(value) && JSON.asList(value) instanceof final List<?> l && !l.isEmpty()) return asNumber(l.get(0), path);
		return null;
	}

	public Number  asNumber (final List<?> p, final int idx, final PathSegment path) throws JsonLogicEvaluationException {
		var value = p.get(idx);
		value = evalNeeded(value) ? evaluate(value, path.sub(idx)) : value;
		if (value instanceof final Number t) return t;
		if (value instanceof final String t) try { return Double.parseDouble(t); } catch (final NumberFormatException e) { return null; }
		if (JSON.isList(value) && JSON.asList(value) instanceof final List<?> l && !l.isEmpty()) return asNumber(l.get(0), path.sub(idx));
		return null;
	}

	public Double  asDouble (final Object p0               , final PathSegment path) throws JsonLogicEvaluationException {
		final var value = evalNeeded(p0) ? evaluate(p0, path) : p0;
		if (value instanceof final Number t) return t.doubleValue();
		if (value instanceof final String t) try { return Double.parseDouble(t); } catch (final NumberFormatException e) { return null; }
		if (JSON.isList(value) && JSON.asList(value) instanceof final List<?> l && !l.isEmpty()) return asDouble(l.get(0), path);
		return null;
	}

	public Double  asDouble (final List<?> p, final int idx, final PathSegment path) throws JsonLogicEvaluationException {
		var value = p.get(idx);
		value = evalNeeded(value) ? evaluate(value, path.sub(idx)) : value;
		if (value instanceof final Number t) return t.doubleValue();
		if (value instanceof final String t) try { return Double.parseDouble(t); } catch (final NumberFormatException e) { return null; }
		if (JSON.isList(value) && JSON.asList(value) instanceof final List<?> l && !l.isEmpty()) return asDouble(l.get(0), path.sub(idx));
		return null;
	}

	public boolean asBoolean(final Object p0               , final PathSegment path) throws JsonLogicEvaluationException {
		return JSON.truthy(evalNeeded(p0) ? evaluate(p0, path) : p0);
	}
}