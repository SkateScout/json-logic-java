package io.github.jamsesso.jsonlogic.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;

public class ArrayLike {

	@SuppressWarnings("unchecked")
	public static List<?> asList(final Object data) {

		if (data instanceof final List t) return Collections.unmodifiableList(t);
		// delegate = ((List<Object>) data).stream().map(JsonLogicEvaluator::transform) .collect(Collectors.toList());
		if (data != null && data.getClass().isArray()) {
			final var ret = new ArrayList<>();
			for (var i = 0; i < Array.getLength(data); i++) ret.add(i, JsonLogicEvaluator.transform(Array.get(data, i)));
			return Collections.unmodifiableList(ret);

		}
		if (data instanceof final Iterable t) {
			final var ret = new ArrayList<>();
			for (final Object item : t) ret.add(JsonLogicEvaluator.transform(item));
			return ret;
		}

		throw new IllegalArgumentException("ArrayLike only works with lists, iterables, arrays, or JsonArray");

	}


	// FIXME add equals
	// @Override
	// public boolean equals(final Object other) {
	// 	if (this == other) return true;
	// 	if (other instanceof Iterable) {
	// 		var i = 0;
	// 		for (final Object item : (Iterable) other) {
	// 			if ((i >= delegate.size()) || !Objects.equals(item, delegate.get(i))) {
	// 				return false;
	// 			}
	//
	// 			i++;
	// 		}
	//
	// 		return(i == delegate.size());
	// 	}
	//
	// 	return false;
	// }

	public static boolean isArray(final Object data) {
		return data != null && (data instanceof Iterable || data.getClass().isArray());
	}
}
