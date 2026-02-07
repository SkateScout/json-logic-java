package io.github.jamsesso.jsonlogic.ast;

import io.github.jamsesso.jsonlogic.PathSegment;

public record Deferred(Object raw, PathSegment jsonPath, Object[] a, int i) { public void accept(final Object v) { a[i] = v; } }