package io.github.jamsesso.jsonlogic;

import java.util.ArrayDeque;

public final class NullableDeque<T> extends ArrayDeque<T>{
	private static final Object NULL = new Object();
	public NullableDeque(final int numElements) { super(numElements); }
	@Override public void push(final T e) { super.push(null==e?(T)NULL:e); }
	@Override public T    pop() { final Object r = super.pop(); return(NULL==r?null:(T)r); }
}