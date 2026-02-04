package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;

public class NumericComparisonExpression implements PreEvaluatedArgumentsExpression {
	public static final NumericComparisonExpression GT = new NumericComparisonExpression(">");
	public static final NumericComparisonExpression GTE = new NumericComparisonExpression(">=");
	public static final NumericComparisonExpression LT = new NumericComparisonExpression("<");
	public static final NumericComparisonExpression LTE = new NumericComparisonExpression("<=");

	private final String key;

	private NumericComparisonExpression(final String key) {
		this.key = key;
	}

	@Override
	public String key() { return key; }

	@Override
	public Object evaluate(final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		// Convert the arguments to doubles
		final var n = Math.min(arguments.size(), 3);

		if (n < 2) {
			throw new JsonLogicEvaluationException("'" + key + "' requires at least 2 arguments", jsonPath);
		}

		final var values = new double[n];

		for (var i = 0; i < n; i++) {
			final var value = arguments.get(i);

			if (value instanceof String) {
				try {
					values[i] = Double.parseDouble((String) value);
				}
				catch (final NumberFormatException e) {
					return false;
				}
			}
			else if (!(value instanceof Number)) {
				return false;
			}
			else {
				values[i] = ((Number) value).doubleValue();
			}
		}

		// Handle between comparisons
		if (arguments.size() >= 3) {
			return switch (key) {
			case "<" -> values[0] < values[1] && values[1] < values[2];
			case "<=" -> values[0] <= values[1] && values[1] <= values[2];
			case ">" -> values[0] > values[1] && values[1] > values[2];
			case ">=" -> values[0] >= values[1] && values[1] >= values[2];
			default -> throw new JsonLogicEvaluationException("'" + key + "' does not support between comparisons", jsonPath);
			};
		}

		// Handle regular comparisons
		return switch (key) {
		case "<" -> values[0] < values[1];
		case "<=" -> values[0] <= values[1];
		case ">" -> values[0] > values[1];
		case ">=" -> values[0] >= values[1];
		default -> throw new JsonLogicEvaluationException("'" + key + "' is not a comparison expression", jsonPath);
		};
	}
}
