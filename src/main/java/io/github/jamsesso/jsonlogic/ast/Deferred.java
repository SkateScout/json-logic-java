package io.github.jamsesso.jsonlogic.ast;

public record Deferred(Object raw, String jsonPath, Object[] a, int i) { public void accept(final Object v) { a[i] = v; } }