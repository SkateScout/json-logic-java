package io.github.jamsesso.jsonlogic.utils;

public final class JsonValueExtractor {
	private JsonValueExtractor() { }

	public static Object extract(final Object element) {
		if(element == null) return null;
		if(element instanceof final String t) return t;
		if(element instanceof final Number t) return t;
		if(element instanceof final Boolean t) return t;
		if(element.getClass().isPrimitive()) return element;
		if(MapLike.isMap(element)) return MapLike.asMap(element); // FIXME map.put(key, extract(object.get(key)));
		if(ArrayLike.isArray(element))return ArrayLike.asList(element); // FIXME values.add(extract(item));
		System.out.println("JsonValueExtractor.extract("+element.getClass().getCanonicalName()+")");
		return element.toString();
	}
}
