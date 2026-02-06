package io.github.jamsesso.jsonlogic.evaluator;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.github.jamsesso.jsonlogic.ast.Deferred;
import io.github.jamsesso.jsonlogic.ast.JSON;
import io.github.jamsesso.jsonlogic.ast.JsonLogicOperation;
import io.github.jamsesso.jsonlogic.ast.JsonLogicVariable;

public record JsonLogicEvaluator(Map<String, JsonLogicExpressionFI> expressions, Object data) {
	public JsonLogicEvaluator { expressions = Collections.unmodifiableMap(expressions); }
	public JsonLogicEvaluator scoped(final Object scopeData) { return new JsonLogicEvaluator(expressions, scopeData); }

	record Value  (Object[] val                 , Deferred d) { void run() { d.accept(val[0]); } }
	record Varable(Object[] key, Object fallback, Deferred d) { }

	record Handler(JsonLogicExpressionFI h, Object[] result, Deferred d, String jsonPath) {
		void run(final JsonLogicEvaluator e) throws JsonLogicEvaluationException {
			final var r = result[0];
			final var args = (JSON.isList(r) ? JSON.asList(r) : Collections.singletonList(r));
			d.accept(h.evaluate(e, args, jsonPath));
		}
	}

	// JsonLogicVariable
	public Object evaluate(final Object logic, final String jsonPath_) throws JsonLogicEvaluationException {
		final var todo   = new LinkedList<>();
		final var result = new Object[1];
		todo.addFirst(new Deferred(logic, jsonPath_, result, 0));
		do {
			final var next      = todo.remove();
			if(next instanceof final Handler   h) { h.run(this); continue; }
			if(next instanceof final Value     h) { h.run(); continue; }
			if(next instanceof final Varable  o) {
				final var key = o.key[0];
				final var res = JsonPath.evaluate(key, o.d.jsonPath(), data);
				if (res != JsonPath.MISSING) { o.d.accept(res); continue; }
				final var ret = new Object[1];
				todo.addFirst(new Value(ret, o.d));
				todo.addFirst(new Deferred(o.fallback, o.d.jsonPath() + "[1]", ret, 0));
				continue;
			}
			if(next instanceof final Deferred cur) {
				final var p0       = cur.raw();
				final var jsonPath = cur.jsonPath();
				if(p0 == null                             ) { cur.accept(null); continue; }
				if(p0 instanceof final Number            t) { cur.accept( t ); continue; }
				if(p0 instanceof final String            t) { cur.accept( t ); continue; }
				if(p0 instanceof final Boolean           t) { cur.accept( t ); continue; }
				if(p0.getClass().isPrimitive()            ) { cur.accept( p0); continue; }
				if(p0 instanceof final List<?>           t) {
					var index = 0;
					final var ret = new Object[t.size()];
					for(final var element : t) { todo.addFirst(new Deferred(element, jsonPath + "[" + index+ "]", ret, index)); index++; }
					cur.accept(Arrays.asList(ret));
					continue;
				}
				if(p0 instanceof final JsonLogicOperation operation) {
					final var handler = expressions.get(operation.operator());
					if (handler == null) throw new JsonLogicEvaluationException("Undefined operation '" + operation.operator() + "'", jsonPath);
					final var args = operation.arguments();
					if(args != null && args.size() == 1 && args.get(0) instanceof final JsonLogicOperation op) {
						final var ret = new Object[1];
						todo.addFirst(new Handler(handler, ret, cur, jsonPath+"."+operation.operator()));
						todo.addFirst(new Deferred(op , jsonPath+" '"+op.operator()+"'", ret, 0));
						continue;
					}
					cur.accept(handler.evaluate(this, args, jsonPath+"."+operation.operator()));
					continue;
				}
				if(p0 instanceof final JsonLogicVariable v) {
					final var res = new Object[1];
					if (null != data) {
						todo.addFirst(new Varable(res, v.defaultValue(), cur));
						todo.addFirst(new Deferred(v.key(), jsonPath, res, 0));
					} else {
						todo.addFirst(new Value(res, cur));
						todo.addFirst(new Deferred(v.defaultValue(), jsonPath + "[1]", res, 0));
					}
					continue;
				}
				throw new IllegalStateException("evaluate({"+p0.getClass().getCanonicalName()+"}"+p0);
			}
			throw new IllegalStateException("evaluate({"+next.getClass().getCanonicalName()+"}"+next);
		} while(!todo.isEmpty());
		return result[0];
	}

	@SuppressWarnings("unchecked")
	public List<Object> evaluate(final List<?> t, final String jsonPath) throws JsonLogicEvaluationException {
		return (List<Object>)evaluate((Object)t, jsonPath);
	}


	public Double asDouble(final Object p0, final String jsonPath) throws JsonLogicEvaluationException {
		final var value = evaluate(p0, jsonPath);
		if (value instanceof final String t) try { return Double.parseDouble(t); } catch (final NumberFormatException e) { return null; }
		if (value instanceof final Number t) return t.doubleValue();
		if (JSON.isList(value) && JSON.asList(value) instanceof final List<?> l && !l.isEmpty()) return asDouble(l.get(0), jsonPath);
		return null;
	}

	public static boolean asBoolean(final Object value) {
		if (value == null) return false;
		if (value instanceof final Boolean v) return v;
		if (value instanceof final Number  n) {
			if (n instanceof final Double  d) { if (d.isNaN     ()) return false; if (d.isInfinite()) return true; }
			if (n instanceof final Float   f) { if (f.isNaN     ()) return false; if (f.isInfinite()) return true; }
			return n.doubleValue() != 0.0;
		}
		if (value instanceof final String s) return !s.isEmpty();
		if (value instanceof final Collection c) return !c.isEmpty();
		if (value.getClass().isArray()) return Array.getLength(value) > 0;
		return true;
	}

	public boolean asBoolean(final Object p0, final String jsonPath) throws JsonLogicEvaluationException { return asBoolean(evaluate(p0, jsonPath)); }
}