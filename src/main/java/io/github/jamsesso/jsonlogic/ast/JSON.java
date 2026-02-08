package io.github.jamsesso.jsonlogic.ast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.google.gson.JsonElement;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.expressions.EqualityExpression;

public class JSON {
	private JSON() { }

	public static Object plain(final Object o) {
		return switch(o) {
		case null                       -> null;
		case final org.json.JSONObject t -> t.toMap();
		case final org.json.JSONArray  t -> t.toList();
		case final String              t -> t;
		case final Number              t -> t;
		case final Boolean             t -> t;
		case final List<?>             t -> t;
		case final Map<?,?>            t -> t;
		case final int[]               t -> t;
		case final JsonElement         t -> {
			if (t.isJsonObject()) {
				final Map<String, Object> map = new HashMap<>();
				final var object = t.getAsJsonObject();
				for (final String key : object.keySet()) {
					map.put(key, plain(object.get(key)));
				}
				yield map;
			}
			if (t.isJsonArray()) {
				final List<Object> values = new ArrayList<>();
				for (final JsonElement item : t.getAsJsonArray()) {
					values.add(plain(item));
				}
				yield values;
			}
			if (t.isJsonNull()) yield null;
			if (t.isJsonPrimitive()) {
				final var primitive = t.getAsJsonPrimitive();
				if (primitive.isBoolean()) yield primitive.getAsBoolean();
				if (primitive.isNumber()) yield primitive.getAsNumber().doubleValue();
				yield primitive.getAsString();
			}
			throw new IllegalStateException("T "+t.getClass().getCanonicalName());
		}
		default -> {
			if(o == org.json.JSONObject.NULL     ) yield null; // Handle null
			throw new IllegalStateException("o "+o.getClass().getCanonicalName());
			// yield o;
		}
		};
	}

	public static Object parse(final String json) throws JsonLogicParseException {
		final var j =json.strip();
		if(j.length() > 0) {
			switch(j.charAt(0)) {
			case 't' -> { if("true" .equals(j)) return true ; }
			case 'f' -> { if("false".equals(j)) return false; }
			case '"' -> { if(j.charAt(j.length()-1)=='"') return j.substring(1,j.length()-1); }
			case '0','1','2','3','4','5','6','7','8','9','-' -> {
				try { return Double .parseDouble(j); } catch(final Throwable _)  {}
			}
			}
		}
		try { return new org.json.JSONObject(json).toMap (); } catch (final Exception e) {  }
		try { return new org.json.JSONArray (json).toList(); } catch (final Exception e) {  }
		return JSONObject.wrap(json);
	}

	public static Map<?,?> asMap(final Object data) {
		if (data instanceof final Map m) return m;
		throw new IllegalArgumentException("MapLike only works with maps and JsonObject");
	}

	public static boolean isMap(final Object data) { return data instanceof Map; }

	@SuppressWarnings("unchecked")
	public static List<Object> asList(final Object data) {
		if (data instanceof final List l) return l;
		if(data instanceof final Object[] l) return Arrays.asList(l);
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