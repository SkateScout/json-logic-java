package io.github.jamsesso.jsonlogic.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicOperation;
import io.github.jamsesso.jsonlogic.ast.JsonLogicVariable;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public class JsonLogicEvaluator {

	/** Sentinel object to represent a missing value (for internal use only). */
	private static final Object MISSING = new Object();

	private final Map<String, JsonLogicExpression> expressions;

	public JsonLogicEvaluator(final Collection<JsonLogicExpression> expressions) {
		this.expressions = new HashMap<>();
		for (final JsonLogicExpression expression : expressions) this.expressions.put(expression.key(), expression);
	}

	public JsonLogicEvaluator(final Map<String, JsonLogicExpression> expressions) {
		this.expressions = Collections.unmodifiableMap(expressions);
	}

	public Object evaluate(final Object node, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if(node == null) return null;
		if(node instanceof final String  t) return t;
		if(node instanceof final Number  t) return t;
		if(node instanceof final Boolean t) return t;
		if(node.getClass().isPrimitive()) return node;
		if(node instanceof final JsonLogicNode t) {
			return switch (t.getType()) {
			case VARIABLE -> evaluate((JsonLogicVariable) node, data, jsonPath + ".var");
			case ARRAY    -> evaluate(node, data, jsonPath);
			default       -> evaluate((JsonLogicOperation) node, data, jsonPath);
			};
		}
		System.out.println("JsonLogicEvaluator.evaluate("+node.getClass().getCanonicalName()+",.,.)");
		return null;
	}

	public Object evaluate(final JsonLogicVariable variable, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		final var defaultValue = evaluate(variable.defaultValue(), null, jsonPath + "[1]");
		if (data == null) return defaultValue;
		final var key = evaluate(variable.key(), data, jsonPath + "[0]");
		if (key == null) return Optional.of(data).map(JsonLogicEvaluator::transform).orElse(evaluate(variable.defaultValue(), null, jsonPath + "[1]"));
		if (key instanceof Number) {
			final var index = ((Number) key).intValue();
			if (ArrayLike.isArray(data)) {
				final var list =ArrayLike.asList(data);
				if (index >= 0 && index < list.size())  return transform(list.get(index));
			}
			return defaultValue;
		}
		// Handle the case when the key is a string, potentially referencing an infinitely-deep map: x.y.z
		if (key instanceof final String name) {
			if (name.isEmpty()) return data;
			final var keys = name.split("\\.");
			var result = data;
			for (final String partial : keys) {
				result = evaluatePartialVariable(partial, result, jsonPath + "[0]");
				if (result == MISSING) return defaultValue;
				if (result == null) return null;
			}
			return result;
		}
		throw new JsonLogicEvaluationException("var first argument must be null, number, or string", jsonPath + "[0]");
	}

	private Object evaluatePartialVariable(final String key, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (ArrayLike.isArray(data)) {
			final var list =ArrayLike.asList(data);
			int index;
			try { index = Integer.parseInt(key); }
			catch (final NumberFormatException e) { throw new JsonLogicEvaluationException(e, jsonPath); }
			if (index < 0 || index >= list.size()) return MISSING;
			return transform(list.get(index));
		}

		if (data instanceof Map) {
			final Map<?, ?> map = (Map<?, ?>) data;
			if (map.containsKey(key)) return transform(map.get(key));
			return MISSING;
		}

		return null;
	}

	public List<?> evaluate(final List<?> array, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		final List<Object> values = new ArrayList<>(array.size());
		var index = 0;
		for(final var element : array) values.add(evaluate(element, data, String.format("%s[%d]", jsonPath, index++)));
		return values;
	}

	public Object evaluate(final JsonLogicOperation operation, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		final var handler = expressions.get(operation.getOperator());
		if (handler == null) throw new JsonLogicEvaluationException("Undefined operation '" + operation.getOperator() + "'", jsonPath);
		return handler.evaluate(this, operation.getArguments(), data, String.format("%s.%s", jsonPath, operation.getOperator()));
	}

	public static Object transform(final Object value) {
		if (value instanceof Number) return ((Number) value).doubleValue();
		return value;
	}
}