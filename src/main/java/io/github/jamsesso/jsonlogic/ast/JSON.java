package io.github.jamsesso.jsonlogic.ast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.google.gson.JsonElement;

import io.github.jamsesso.jsonlogic.NumericDouble;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;

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
			if(o.getClass().isArray()) yield JSON.asList(o);
			throw new IllegalStateException("o "+o.getClass().getCanonicalName());
			// yield o;
		}
		};
	}

	public static Object parse(final String json) throws JsonLogicParseException {
		try {
			if(true) return JsonParserString.parse(json);
		} catch(final Throwable t) {
			System.out.println(json+" => "+t.getMessage());
			t.printStackTrace();
		}
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
			// System.out.println("asList({"+data.getClass().getCanonicalName()+"})");
			final var len = Array.getLength(data);
			final var l   = new ArrayList<>(len);
			for (var i = 0; i < len; i++) {
				var v = Array.get(data, i);
				if(v instanceof final Integer t) v = t.doubleValue();
				l.add(i, v);
			}
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
			if (!equalityValue(item_b, item_a)) return false;
			i++;
		}
		return true;
	}

	public static boolean isList(final Object data) { return data != null && (data instanceof Iterable || data.getClass().isArray()); }

	public static boolean truthy(final Object v) {
		return switch(v) {
		case null                  -> false;
		case final String        t -> !t.isEmpty();
		case final Boolean       t ->  t;
		case final Collection<?> t -> !t.isEmpty();
		case final Double        t -> !t.isNaN() && (t.isInfinite() || t.doubleValue() != 0.0);
		case final Float         t -> !t.isNaN() && (t.isInfinite() || t.floatValue () != 0.0);
		case final Number        t ->  t.doubleValue() != 0.0;
		default                    -> !v.getClass().isArray() || Array.getLength(v) > 0;
		};
	}

	private static boolean compareNumberToString(final Number left, final String right) {
		try { return (right.trim().isEmpty()) ? 0 == left.doubleValue() : Double.parseDouble(right) == left.doubleValue(); }
		catch (final NumberFormatException e) { return false; }
	}

	private static boolean compareNumberToBoolean(final Number left, final Boolean right) { return (right ? left.doubleValue() == 1.0 : left.doubleValue() == 0.0); }
	private static boolean compareStringToBoolean(final String left, final Boolean right) { return JSON.truthy(left) == right; }

	public static boolean equalityValue(final Object left, final Object right) throws JsonLogicEvaluationException {
		// Use the loose equality matrix
		// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Equality_comparisons_and_sameness#Loose_equality_using
		if ((left ==         right) || (left == null && right == null)) return true;
		if (left == null || right == null) return false;
		// Check numeric loose equality
		if (left instanceof final Number  l && right instanceof final Number  r) return NumericDouble.ONCE.EQ (l, r);
		if (left instanceof final Number  l && right instanceof final String  r) return compareNumberToString (l, r);
		if (left instanceof final Number  l && right instanceof final Boolean r) return compareNumberToBoolean(l, r);
		// Check string loose equality
		if (left instanceof final String  l && right instanceof final String  r) return l.equals(r);
		if (left instanceof final String  l && right instanceof final Number  r) return compareNumberToString (r, l);
		if (left instanceof final String  l && right instanceof final Boolean r) return compareStringToBoolean(l, r);
		// Check boolean loose equality
		if (left instanceof final Boolean l && right instanceof final Boolean r) return r.booleanValue() == l.booleanValue();
		if (left instanceof final Boolean l && right instanceof final Number  r) return compareNumberToBoolean(r, l);
		if (left instanceof final Boolean l && right instanceof final String  r) return compareStringToBoolean(r, l);
		if (left instanceof final List<?> l && right instanceof final List<?> r) return JSON.equals(r, l);
		// Check non-truthy values
		return !JSON.truthy(left) && !JSON.truthy(right);
	}

}