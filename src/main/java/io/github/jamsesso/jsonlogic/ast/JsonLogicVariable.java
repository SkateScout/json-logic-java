package io.github.jamsesso.jsonlogic.ast;

public record JsonLogicVariable(Object[] args) implements JsonLogicNode {
	public Object key         () { return args.length > 0 ? args[0] : null; }
	public Object defaultValue() { return args.length > 1 ? args[1] : null; }
	@Override public String toString() { return "LOGIK.VAR("+key()+","+defaultValue()+")"; }
}