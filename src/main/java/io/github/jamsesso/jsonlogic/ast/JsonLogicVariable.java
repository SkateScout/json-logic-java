package io.github.jamsesso.jsonlogic.ast;

public record JsonLogicVariable(Object key, Object defaultValue) implements JsonLogicNode {
	@Override public JsonLogicNodeType getType() { return JsonLogicNodeType.VARIABLE; }

}
