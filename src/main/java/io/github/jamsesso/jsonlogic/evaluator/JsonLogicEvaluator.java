package io.github.jamsesso.jsonlogic.evaluator;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.jamsesso.jsonlogic.ast.JSON;
import io.github.jamsesso.jsonlogic.ast.JsonLogicOperation;
import io.github.jamsesso.jsonlogic.ast.JsonLogicVariable;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public record JsonLogicEvaluator(Map<String, JsonLogicExpressionFI> expressions) {

	/** Sentinel object to represent a missing value (for internal use only). */
	private static final Object MISSING = new Object();

	public JsonLogicEvaluator(final Collection<JsonLogicExpression> expressions) {
		var map = new HashMap<String, JsonLogicExpressionFI>();
		for (JsonLogicExpression expression : expressions) map.put(expression.key(), expression);
		this(map);
	}

	public JsonLogicEvaluator(final Map<String, JsonLogicExpressionFI> expressions) { this.expressions = Collections.unmodifiableMap(expressions); }

	public List<Object> evaluate(final List<?> t, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		List<Object> values = new ArrayList<>(t.size());
		int index = 0;
		for(var element : t) values.add(evaluate(element, data, String.format("%s[%d]", jsonPath, index++)));
		return values;
	}

	public Object evaluateVar(final JsonLogicVariable p0, final Object data, String jsonPath) throws JsonLogicEvaluationException {
		jsonPath = jsonPath + ".var";
		if (data == null) return evaluate(p0.defaultValue(), null, jsonPath + "[1]");
		var key = evaluate(p0.key(), data, jsonPath + "[0]");
		if (key == null) return Optional.of(data).map(JsonLogicEvaluator::transform).orElse(evaluate(p0.defaultValue(), null, jsonPath + "[1]"));

		if (key instanceof Number idx) {
			int index = idx.intValue();
			if (ArrayLike.isList(data)) {
				var list = ArrayLike.asList(data);
				if (index >= 0 && index < list.size()) return transform(list.get(index));
			}
			return evaluate(p0.defaultValue(), null, jsonPath + "[1]");
		}

		// Handle the case when the key is a string, potentially referencing an infinitely-deep map: x.y.z
		if (key instanceof String name) {
			if (name.isEmpty()) return data;
			var keys = name.split("\\.");
			Object result = data;
			for (var partial : keys) {
				result = evaluatePartialVariable(partial, result, jsonPath + "[0]");
				if (result == MISSING) return evaluate(p0.defaultValue(), null, jsonPath + "[1]");
				if (result == null) return null;
			}
			return result;
		}
		throw new JsonLogicEvaluationException("var first argument must be null, number, or string", jsonPath + "[0]");
	}

	// JsonLogicVariable
	public Object evaluate(final Object p0, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if(p0 == null                      ) return null;
		if(p0 instanceof Number            t) return t;
		if(p0 instanceof String            t) return t;
		if(p0 instanceof Boolean           t) return t;
		if(p0 instanceof List<?>           t) return evaluate(t, data, jsonPath);
		if(ArrayLike.isList(p0)) return ArrayLike.asList(p0);
		if(p0.getClass().isPrimitive()      ) return p0;
		if(p0 instanceof JsonLogicOperation operation) {
			JsonLogicExpressionFI handler = expressions.get(operation.operator());
			if (handler == null) throw new JsonLogicEvaluationException("Undefined operation '" + operation.operator() + "'", jsonPath);
			var args = operation.arguments();
			if(args != null && args.size() == 1 && args.get(0) instanceof JsonLogicOperation op) {
				var ret = evaluate(op, data, jsonPath+" '"+op.operator()+"'");
				if(ArrayLike.isList(ret)) args = ArrayLike.asList(ret);
				else                      args = Collections.singletonList(ret);
			}
			return handler.evaluate(this, args, data, String.format("%s.%s", jsonPath, operation.operator()));
		}
		if(p0 instanceof JsonLogicVariable var) return evaluateVar(var, data, jsonPath);
		// if(p0 instanceof Map<?,?> map) return evaluate(JsonLogicParser.parseMap(map, jsonPath), data, jsonPath);
		throw new IllegalStateException("evaluate({"+p0.getClass().getCanonicalName()+"}"+p0);
	}

	public Double asDouble(final Object p0, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		var value = evaluate(p0, data, jsonPath);
		if (value instanceof String t) try { return Double.parseDouble(t); } catch (NumberFormatException e) { return null; }
		if (value instanceof Number t) return t.doubleValue();
		return null;
	}

	public static boolean asBoolean(final Object value) {
		if (value == null) return false;
		if (value instanceof Boolean v) return v;
		if (value instanceof Number n) {
			if (n instanceof Double d) {
				if (d.isNaN     ()) return false;
				if (d.isInfinite()) return true;
			}

			if (n instanceof Float f) {
				if (f.isNaN     ()) return false;
				if (f.isInfinite()) return true;
			}
			return n.doubleValue() != 0.0;
		}
		if (value instanceof String s) return !s.isEmpty();
		if (value instanceof Collection c) return !c.isEmpty();
		if (value.getClass().isArray()) return Array.getLength(value) > 0;
		return true;
	}

	public boolean asBoolean(final Object p0, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		return asBoolean(evaluate(p0, data, jsonPath));
	}

	private static Object evaluatePartialVariable(final String key,  final Object raw, final String jsonPath) throws JsonLogicEvaluationException {
		if(raw instanceof JsonLogicOperation) throw new IllegalArgumentException("JsonLogicOperation");
		var data = JSON.plain(raw);
		if (ArrayLike.isList(data)) {
			var list = ArrayLike.asList(data);
			int index;
			try { index = Integer.parseInt(key); }
			catch (NumberFormatException e) {
				System.out.println("KEY: "+key+" RAW{"+raw.getClass().getCanonicalName()+"}: "+raw);
				throw new JsonLogicEvaluationException(e, jsonPath);
			}
			if (index < 0 || index >= list.size()) return MISSING;
			return transform(list.get(index));
		}
		if (data instanceof Map map) {
			if (map.containsKey(key)) return transform(map.get(key));
			return MISSING;
		}
		if(data == null) return MISSING;
		System.out.println("evaluatePartialVariable() "+data.getClass().getCanonicalName());
		return null;
	}

	public static Object transform(final Object value) { return (value instanceof Number t ? t.doubleValue() : value); }
}