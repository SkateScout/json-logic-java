package io.github.jamsesso.jsonlogic.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.expressions.EqualityExpression;

public class ArrayLike {

	@SuppressWarnings("unchecked")
	public static List<Object> asList(final Object data) {
		if (data instanceof List l) return l;
		if (data != null && data.getClass().isArray()) {
			var len = Array.getLength(data);
			var l = new ArrayList<>(len);
			for (int i = 0; i < len; i++) l.add(i, JsonLogicEvaluator.transform(Array.get(data, i)));
			return l;
		}
		if (data instanceof Iterable iter) {
			var l = new ArrayList<>();
			for (Object item : iter) l.add(JsonLogicEvaluator.transform(item));
			return l;
		}
		throw new IllegalArgumentException("ArrayLike only works with lists, iterables, arrays, or JsonArray");
	}


	public static boolean equals(final List<?> a, final List<?> b) throws JsonLogicEvaluationException {
		if(a == b) return true;
		if(a == null || b == null) return false;
		if(a.size() != b.size()) return false;
		int i = 0;
		for (var item_b : b) {
			var  item_a = a.get(i);
			if (!EqualityExpression.equalityValue(item_b, item_a)) return false;
			i++;
		}
		return true;
	}

	public static boolean isList(final Object data) { return data != null && (data instanceof Iterable || data.getClass().isArray()); }
}