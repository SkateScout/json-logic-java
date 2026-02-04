package io.github.jamsesso.jsonlogic.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapLike implements Map<Object, Object> {
	private final Map<Object, Object> delegate;

	@SuppressWarnings("unchecked")
	public MapLike(final Object data) {
		if (data instanceof Map m) delegate = m;
		// else if (data instanceof com.google.gson.JsonObject) delegate = (Map) JsonValueExtractor.extract((JsonObject) data);
		else throw new IllegalArgumentException("MapLike only works with maps and JsonObject");
	}

	@Override public int size() { return delegate.size(); }
	@Override public boolean isEmpty() { return delegate.isEmpty(); }
	@Override public boolean containsKey(final Object key) { return delegate.containsKey(key); }
	@Override public boolean containsValue(final Object value) { return delegate.containsKey(value); }
	@Override public Object get(final Object key) { return delegate.get(key); }
	@Override public Object put(final Object key, final Object value) { throw new UnsupportedOperationException("MapLike is immutable"); }
	@Override public Object remove(final Object key) { throw new UnsupportedOperationException("MapLike is immutable"); }
	@Override public void putAll(final Map<?, ?> m) { throw new UnsupportedOperationException("MapLike is immutable"); }
	@Override public void clear() { throw new UnsupportedOperationException("MapLike is immutable"); }
	@Override public Set<Object> keySet() { return delegate.keySet(); }
	@Override public Collection<Object> values() { return delegate.values(); }
	@Override public Set<Entry<Object, Object>> entrySet() { return delegate.entrySet(); }
	// public static boolean isEligible(Object data) { return data instanceof Map || data instanceof JsonObject; }
	public static boolean isEligible(final Object data) { return data instanceof Map; }

}
