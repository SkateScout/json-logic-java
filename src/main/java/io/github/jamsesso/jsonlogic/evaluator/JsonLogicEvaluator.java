package io.github.jamsesso.jsonlogic.evaluator;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.jamsesso.jsonlogic.ast.JSON;
import io.github.jamsesso.jsonlogic.ast.JsonLogicOperation;
import io.github.jamsesso.jsonlogic.ast.JsonLogicVariable;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public record JsonLogicEvaluator(Map<String, JsonLogicExpressionFI> expressions, Object data) {

	public JsonLogicEvaluator { expressions = Collections.unmodifiableMap(expressions); }

	public JsonLogicEvaluator scoped(final Object scopeData) { return new JsonLogicEvaluator(expressions, scopeData); }

	/** Sentinel object to represent a missing value (for internal use only). */
	private static final Object MISSING = new Object();

	public List<Object> evaluate(final List<?> t, final String jsonPath) throws JsonLogicEvaluationException {
		var index = 0;
		final var ret = new Object[t.size()];
		for(final var element : t) { ret[index] = evaluate(element, String.format("%s[%d]", jsonPath, index)); index++; }
		return Arrays.asList(ret);
	}

	public Object evaluateVar(final JsonLogicVariable p0, String jsonPath) throws JsonLogicEvaluationException {
		jsonPath = jsonPath + ".var";
		if (data == null) return evaluate(p0.defaultValue(), jsonPath + "[1]");
		final var key = evaluate(p0.key(), jsonPath + "[0]");
		if (key == null) return Optional.of(data).orElse(evaluate(p0.defaultValue(), jsonPath + "[1]"));

		if (key instanceof final Number idx) {
			final var index = idx.intValue();
			if (ArrayLike.isList(data)) {
				final var list = ArrayLike.asList(data);
				if (index >= 0 && index < list.size()) return list.get(index);
			}
			return evaluate(p0.defaultValue(), jsonPath + "[1]");
		}

		// Handle the case when the key is a string, potentially referencing an infinitely-deep map: x.y.z
		if (key instanceof final String name) {
			if (name.isEmpty()) return data;
			final var keys = name.split("\\.");
			var result = data;
			for (final var partial : keys) {
				result = evaluatePartialVariable(partial, result, jsonPath + "[0]");
				if (result == MISSING) return evaluate(p0.defaultValue(), jsonPath + "[1]");
				if (result == null) return null;
			}
			return result;
		}
		throw new JsonLogicEvaluationException("var first argument must be null, number, or string", jsonPath + "[0]");
	}

	// JsonLogicVariable
	public Object evaluate(final Object p0, final String jsonPath) throws JsonLogicEvaluationException {
		if(p0 == null                      ) return null;
		if(p0 instanceof final Number            t) return t;
		if(p0 instanceof final String            t) return t;
		if(p0 instanceof final Boolean           t) return t;
		if(p0 instanceof final List<?>           t) return evaluate(t,  jsonPath);
		if(ArrayLike.isList(p0)) return ArrayLike.asList(p0);
		if(p0.getClass().isPrimitive()      ) return p0;
		if(p0 instanceof final JsonLogicOperation operation) {
			final var handler = expressions.get(operation.operator());
			if (handler == null) throw new JsonLogicEvaluationException("Undefined operation '" + operation.operator() + "'", jsonPath);
			var args = operation.arguments();
			if(args != null && args.size() == 1 && args.get(0) instanceof final JsonLogicOperation op) {
				final var ret = evaluate(op, jsonPath+" '"+op.operator()+"'");
				if(ArrayLike.isList(ret)) args = ArrayLike.asList(ret);
				else                      args = Collections.singletonList(ret);
			}
			return handler.evaluate(this, args, String.format("%s.%s", jsonPath, operation.operator()));
		}
		if(p0 instanceof final JsonLogicVariable var) return evaluateVar(var, jsonPath);
		// if(p0 instanceof Map<?,?> map) return evaluate(JsonLogicParser.parseMap(map, jsonPath), data, jsonPath);
		throw new IllegalStateException("evaluate({"+p0.getClass().getCanonicalName()+"}"+p0);
	}

	public Double asDouble(final Object p0, final String jsonPath) throws JsonLogicEvaluationException {
		final var value = evaluate(p0, jsonPath);
		if (value instanceof final String t) try { return Double.parseDouble(t); } catch (final NumberFormatException e) { return null; }
		if (value instanceof final Number t) return t.doubleValue();
		if (ArrayLike.isList(value) && ArrayLike.asList(value) instanceof final List<?> l && !l.isEmpty()) return asDouble(l.get(0), jsonPath);
		return null;
	}

	public static boolean asBoolean(final Object value) {
		if (value == null) return false;
		if (value instanceof final Boolean v) return v;
		if (value instanceof final Number n) {
			if (n instanceof final Double d) {
				if (d.isNaN     ()) return false;
				if (d.isInfinite()) return true;
			}

			if (n instanceof final Float f) {
				if (f.isNaN     ()) return false;
				if (f.isInfinite()) return true;
			}
			return n.doubleValue() != 0.0;
		}
		if (value instanceof final String s) return !s.isEmpty();
		if (value instanceof final Collection c) return !c.isEmpty();
		if (value.getClass().isArray()) return Array.getLength(value) > 0;
		return true;
	}

	public boolean asBoolean(final Object p0, final String jsonPath) throws JsonLogicEvaluationException { return asBoolean(evaluate(p0, jsonPath)); }

	private static Object evaluatePartialVariable(final String key,  final Object raw, final String jsonPath) throws JsonLogicEvaluationException {
		if(raw instanceof JsonLogicOperation) throw new IllegalArgumentException("JsonLogicOperation");
		final var data = JSON.plain(raw);
		if (ArrayLike.isList(data)) {
			final var list = ArrayLike.asList(data);
			int index;
			try { index = Integer.parseInt(key); }
			catch (final NumberFormatException e) { throw new JsonLogicEvaluationException(e, jsonPath); }
			if (index < 0 || index >= list.size()) return MISSING;
			return list.get(index);
		}
		if (data instanceof final Map map) return (map.containsKey(key) ? map.get(key) : MISSING);
		if(data == null) return MISSING;
		System.out.println("evaluatePartialVariable() "+data.getClass().getCanonicalName());
		return null;
	}
}