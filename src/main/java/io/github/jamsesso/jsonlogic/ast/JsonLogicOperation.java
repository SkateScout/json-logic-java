package io.github.jamsesso.jsonlogic.ast;

import java.util.List;

public record JsonLogicOperation(String operator, List<?> arguments) implements JsonLogicNode { }
