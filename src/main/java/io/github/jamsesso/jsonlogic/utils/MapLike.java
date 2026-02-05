package io.github.jamsesso.jsonlogic.utils;

import java.util.Map;

public class MapLike {
	public static Map<?,?> asMap(final Object data) {
		if (!(data instanceof final Map m)) throw new IllegalArgumentException("MapLike only works with maps and JsonObject");
		return m;
	}

	public static boolean isEligible(final Object data) { return data instanceof Map; }
}
