package io.github.jamsesso.jsonlogic.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.expressions.EqualityExpression;

public class ArrayLike {

	@SuppressWarnings("unchecked")
	public static List<Object> asList(final Object data) {
		if (data instanceof final List l) return l;
		if (data != null && data.getClass().isArray()) {
			System.out.println("asList({"+data.getClass().getCanonicalName()+"})");
			final var len = Array.getLength(data);
			final var l   = new ArrayList<>(len);
			for (var i = 0; i < len; i++) l.add(i, Array.get(data, i));
			return l;
		}
		if (data instanceof final Iterable iter) {
			final var l = new ArrayList<>();
			for (final Object item : iter) l.add(item);
			return l;
		}
		throw new IllegalArgumentException("ArrayLike only works with lists, iterables, arrays, or JsonArray");
	}


	public static boolean equals(final List<?> a, final List<?> b) throws JsonLogicEvaluationException {
		if(a == b) return true;
		if(a == null || b == null || (a.size() != b.size())) return false;
		var i = 0;
		for (final var item_b : b) {
			final var  item_a = a.get(i);
			if (!EqualityExpression.equalityValue(item_b, item_a)) return false;
			i++;
		}
		return true;
	}

	public static boolean isList(final Object data) { return data != null && (data instanceof Iterable || data.getClass().isArray()); }
}