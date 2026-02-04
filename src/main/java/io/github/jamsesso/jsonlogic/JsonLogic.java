package io.github.jamsesso.jsonlogic;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.AllExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.ArrayHasExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.ConcatenateExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.EqualityExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.FilterExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.IfExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.InExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.InequalityExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.LogExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.LogicExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.MapExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.MathExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.MergeExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.MissingExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.NotExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.NumericComparisonExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.ReduceExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.StrictEqualityExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.StrictInequalityExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.SubstringExpression;

public final class JsonLogic {
	private final Map<String, Object> parseCache = new ConcurrentHashMap<>();
	private final Map<String, JsonLogicExpression> expressions = new ConcurrentHashMap<>();
	private JsonLogicEvaluator evaluator;

	public JsonLogic() {
		// Add default operations
		addOperation(MathExpression.ADD);
		addOperation(MathExpression.SUBTRACT);
		addOperation(MathExpression.MULTIPLY);
		addOperation(MathExpression.DIVIDE);
		addOperation(MathExpression.MODULO);
		addOperation(MathExpression.MIN);
		addOperation(MathExpression.MAX);
		addOperation(NumericComparisonExpression.GT);
		addOperation(NumericComparisonExpression.GTE);
		addOperation(NumericComparisonExpression.LT);
		addOperation(NumericComparisonExpression.LTE);
		addOperation(IfExpression.IF);
		addOperation(IfExpression.TERNARY);
		addOperation(EqualityExpression.INSTANCE);
		addOperation(InequalityExpression.INSTANCE);
		addOperation(StrictEqualityExpression.INSTANCE);
		addOperation(StrictInequalityExpression.INSTANCE);
		addOperation(NotExpression.SINGLE);
		addOperation(NotExpression.DOUBLE);
		addOperation(LogicExpression.AND);
		addOperation(LogicExpression.OR);
		addOperation(LogExpression.STDOUT);
		addOperation(MapExpression.INSTANCE);
		addOperation(FilterExpression.INSTANCE);
		addOperation(ReduceExpression.INSTANCE);
		addOperation(AllExpression.INSTANCE);
		addOperation(ArrayHasExpression.SOME);
		addOperation(ArrayHasExpression.NONE);
		addOperation(MergeExpression.INSTANCE);
		addOperation(InExpression.INSTANCE);
		addOperation(ConcatenateExpression.INSTANCE);
		addOperation(SubstringExpression.INSTANCE);
		addOperation(MissingExpression.ALL);
		addOperation(MissingExpression.SOME);
	}

	public JsonLogic addOperation(final String name, final Function<Object[], Object> function) {
		return addOperation(new PreEvaluatedArgumentsExpression() {
			@Override
			public Object evaluate(final List arguments, final Object data, final String jsonPath) {
				return function.apply(arguments.toArray());
			}

			@Override
			public String key() {
				return name;
			}
		});
	}

	public JsonLogic addOperation(final JsonLogicExpression expression) {
		expressions.put(expression.key(), expression);
		evaluator = null;

		return this;
	}

	public Object apply(final String json, final Object data) throws JsonLogicException {
		if (!parseCache.containsKey(json)) {
			parseCache.put(json, JsonLogicParser.parse(json));
		}

		if (evaluator == null) {
			evaluator = new JsonLogicEvaluator(expressions);
		}

		return evaluator.evaluate(parseCache.get(json), data, "$");
	}

	public static boolean truthy(final Object value) {
		if (value == null) {
			return false;
		}

		if (value instanceof Boolean) {
			return (boolean) value;
		}

		if (value instanceof Number) {
			if (value instanceof final Double d) {
				if (d.isNaN()) {
					return false;
				}
				if (d.isInfinite()) {
					return true;
				}
			}

			if (value instanceof final Float f) {
				if (f.isNaN()) {
					return false;
				}
				if (f.isInfinite()) {
					return true;
				}
			}

			return ((Number) value).doubleValue() != 0.0;
		}

		if (value instanceof String) {
			return !((String) value).isEmpty();
		}

		if (value instanceof Collection) {
			return !((Collection) value).isEmpty();
		}

		if (value.getClass().isArray()) {
			return Array.getLength(value) > 0;
		}

		return true;
	}
}
