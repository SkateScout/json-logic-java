package io.github.jamsesso.jsonlogic.evaluator.expressions;

import java.io.PrintStream;
import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;

public class LogExpression implements PreEvaluatedArgumentsExpression {
	public static final LogExpression STDOUT = new LogExpression(System.out);

	private final PrintStream printer;

	public LogExpression(final PrintStream printer) {
		this.printer = printer;
	}

	@Override
	public String key() {
		return "log";
	}

	@Override
	public Object evaluate(final List<?> arguments,final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.isEmpty()) {
			throw new JsonLogicEvaluationException("log operator requires exactly 1 argument", jsonPath);
		}

		final var value = arguments.get(0);
		printer.println("JsonLogic: " + value);

		return value;
	}
}
