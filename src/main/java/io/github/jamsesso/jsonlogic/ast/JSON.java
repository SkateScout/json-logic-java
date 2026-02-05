package io.github.jamsesso.jsonlogic.ast;

public class JSON {
	private JSON() { }

	public static Object plain(final Object o) {
		if(o instanceof final org.json.JSONObject t) return t.toMap();
		if(o instanceof final org.json.JSONArray  t) return t.toList();
		if(o == org.json.JSONObject.NULL     ) return null; // Handle null
		return o;
	}

	public static Object parse(final String json) throws JsonLogicParseException {
		try { return new org.json.JSONObject(json).toMap (); } catch (final Exception e) {  }
		try { return new org.json.JSONArray (json).toList(); } catch (final Exception e) {  }
		return json;
	}
}