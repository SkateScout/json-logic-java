package io.github.jamsesso.jsonlogic.ast;

import java.util.List;

public class JsonLogicOperation implements JsonLogicNode {
	private final String operator;
	private final List<?> arguments;

	public JsonLogicOperation(final String operator, final List<?> arguments) {
		this.operator = operator;
		this.arguments = arguments;
	}

	@Override
	public JsonLogicNodeType getType() {
		return JsonLogicNodeType.OPERATION;
	}

	public String getOperator() {
		return operator;
	}

	public List<?> getArguments() {
		return arguments;
	}
}
