package io.github.jamsesso.jsonlogic;

import java.util.ArrayDeque;

public final class NullableDeque<T> extends ArrayDeque<Object>{
	private static final long serialVersionUID = 1L;
	private static final Object NULL = new Object();
	public NullableDeque(final int numElements) { super(numElements); }

	@Override public void push(final Object e) {
		super.push(e == null ? NULL : e);
	}

	@SuppressWarnings("unchecked")
	@Override public T    pop() {
		final var r = super.pop();
		return (r == NULL ? null : (T)r);
	}
}