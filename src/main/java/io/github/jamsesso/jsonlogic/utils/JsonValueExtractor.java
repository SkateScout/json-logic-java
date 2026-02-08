package io.github.jamsesso.jsonlogic.utils;

import io.github.jamsesso.jsonlogic.ast.JSON;

public class JsonValueExtractor {
	public static Object extract(final Object object) {
		return JSON.plain(object);
	}
}
