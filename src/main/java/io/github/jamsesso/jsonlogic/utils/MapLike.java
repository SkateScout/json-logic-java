package io.github.jamsesso.jsonlogic.utils;

import java.util.Collections;
import java.util.Map;

public class MapLike {

	@SuppressWarnings("unchecked")
	public static Map<?,?> asMap(final Object data) {
		if (data instanceof final Map t) return Collections.unmodifiableMap(t);
		throw new IllegalArgumentException("MapLike only works with maps and JsonObject");
	}


	public static boolean isMap(final Object data) {
		return data instanceof Map;
	}
}
