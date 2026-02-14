package io.github.jamsesso.jsonlogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.jamsesso.jsonlogic.ast.JSON;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpressionFI;
import io.github.jamsesso.jsonlogic.evaluator.expressions.MissingExpression;

public final class JsonLogic {
	private record MetaExpr(String name, int minArgs, int maxArgs, JsonLogicExpressionFI e) implements JsonLogicExpressionFI {
		MetaExpr { if(maxArgs==0) maxArgs = Integer.MAX_VALUE; }
		@Override public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
			final var size = arguments.size();
			if(size < minArgs || size > maxArgs) {
				final var c0 = name.charAt(0);
				final var op = c0<'a' || c0>'z' ? "'"+name+"'" : name;
				if(minArgs == 1 && maxArgs == 1) throw new JsonLogicEvaluationException(op + " expects single argument"                 , path);
				if(minArgs +  1 == maxArgs     ) throw new JsonLogicEvaluationException(op + " expects "+minArgs+" or "+maxArgs+" arguments", path);
				if(minArgs ==      maxArgs     ) throw new JsonLogicEvaluationException(op + " expects exactly "  +minArgs+" arguments", path);
				if(size >          maxArgs     ) throw new JsonLogicEvaluationException(op + " expects at most " +maxArgs+" arguments", path);
				if(size < 1                    ) throw new JsonLogicEvaluationException(op + " expects at least "+minArgs+" argument", path);
				if(size <          minArgs     ) throw new JsonLogicEvaluationException(op + " expects at least "+minArgs+" arguments", path);
				// if (isSome && (args.size() < 2)) throw new JsonLogicEvaluationException("'"+name + "' expects first argument to be an integer and the second argument to be an array", jsonPath);
			}
			return e.evaluate(evaluator, arguments, path);
		}
	}

	private static final Map<String, JsonLogicExpressionFI> defaultExpressions;
	public  static final INumeric NUMBER = NumericDouble.ONCE;
	private static final Map<Object, Object> parseCache  = new LinkedHashMap<>(512, 0.75f, true) {
		@Override protected boolean removeEldestEntry(final Entry<Object, Object> eldest) { return size()>500; }
	};

	private              Map<String, JsonLogicExpressionFI> expressions;
	public               INumeric number = NUMBER;

	public JsonLogic() { expressions = defaultExpressions; }

	private static void addOperation(final Map<String, JsonLogicExpressionFI> e, final String key, final JsonLogicExpressionFI fkt) { e.put(key, fkt); }
	private static void addOperation(final Map<String, JsonLogicExpressionFI> e, final String key, final int minArgs, final int maxArgs, final JsonLogicExpressionFI fkt) {
		e.put(key, new MetaExpr(key, minArgs, maxArgs, fkt));
	}

	private static void addListOperation(final Map<String, JsonLogicExpressionFI> e, final String name, final Function<List<?>, Object> function) {
		addOperation(e, name, (JsonLogicExpressionFI) (evaluator, arguments, jsonPath) -> {
			var values = evaluator.evaluate(arguments, jsonPath);
			if (values.size() == 1 && JSON.isList(values.get(0))) values = JSON.asList(values.get(0));
			return function.apply(arguments);
		});
	}

	static {
		final var m = new HashMap<String, JsonLogicExpressionFI>();
		addOperation    (m, "if"                , JsonLogic::ifExpt);	// IF
		addOperation    (m, "?:"                , JsonLogic::ifExpt);	// TERNARY

		addOperation    (m, ">"           , 2, 0, (ev, args, path) -> evaluateBoolean(">" , (a,b)->(a >  b), ev, args, path));
		addOperation    (m, ">="          , 2, 0, (ev, args, path) -> evaluateBoolean(">=", (a,b)->(a >= b), ev, args, path));
		addOperation    (m, "<"           , 2, 0, (ev, args, path) -> evaluateBoolean("<" , (a,b)->(a <  b), ev, args, path));
		addOperation    (m, "<="          , 2, 0, (ev, args, path) -> evaluateBoolean("<=", (a,b)->(a <= b), ev, args, path));

		addOperation    (m, "!"           , 0, 0, (ev, args, path) -> {
			if(args.isEmpty()) return true;
			if(args.size()>1) throw new JsonLogicEvaluationException("'!' expects single argument", path);
			return ! ev.asBoolean(args.get(0), path.sub(0));
		});

		addOperation    (m, "!!"          , 1, 1, (ev, args, path) -> args.isEmpty() ? true  :   ev.asBoolean(args.get(0), path.sub(0)));
		addOperation    (m, "and"         , 1, 0, (ev, args, path) -> andOr       (true , ev, args, path));
		addOperation    (m, "or"          , 1, 0, (ev, args, path) -> andOr       (false, ev, args, path));
		addOperation    (m, "some"        , 2, 2, (ev, args, path) -> has         (true , ev, args, path));
		addOperation    (m, "none"        , 2, 2, (ev, args, path) -> has         (false, ev, args, path));
		addOperation    (m, "!="          , 2, 2, (ev, args, path) -> !JsonLogic.equality(ev, args, path));
		addOperation    (m, "=="          , 2, 2, (JsonLogicExpressionFI) JsonLogic::equality);
		addOperation    (m, "!=="         , 2, 2, (ev, args, path)->!strictEquality(ev, args, path));
		addOperation    (m, "==="         , 2, 2, JsonLogic::strictEquality);
		addOperation    (m, "map"         , 2, 2, JsonLogic       ::map);
		addOperation    (m, "filter"      , 2, 2, JsonLogic       ::filter);
		addOperation    (m, "reduce"      , 3, 3, JsonLogic       ::reduce);
		addOperation    (m, "all"         , 2, 2, JsonLogic       ::all );
		addOperation    (m, "substr"      , 2, 3, JsonLogic       ::substr);
		addOperation    (m, "missing_some", 2, 0, new MissingExpression(true ));
		addOperation    (m, "log"         , 1, 1, JsonLogic       ::log);
		addOperation    (m, "in"                , JsonLogic       ::in);
		addOperation    (m, "-"                 , new Reduce(NUMBER::MINUS  , 0, 1, true ));
		addOperation    (m, "+"                 , new Reduce(NUMBER::SUM    , 0, 1, false));
		addOperation    (m, "*"                 , new Reduce(NUMBER::MUL    , 0, 1, false));
		addOperation    (m, "/"                 , new Reduce(NUMBER::DIV    , 2, 2, false));
		addOperation    (m, "%"                 , new Reduce(NUMBER::MOD    , 2, 2, false));
		addOperation    (m, "min"               , new Reduce(NUMBER::MIN    , 0, 1, false));
		addOperation    (m, "max"               , new Reduce(NUMBER::MAX    , 0, 1, false));
		addOperation    (m, "missing"           , new MissingExpression(false));
		addOperation    (m, "merge"             , JsonLogic::merge);
		addListOperation(m, "cat"               , a->a.stream().map(o -> o instanceof final Double t && t.toString().endsWith(".0") ? t.intValue() : o).map(Object::toString).collect(Collectors.joining()));
		defaultExpressions = m;
	}

	public static boolean equality(final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment jsonPath) throws JsonLogicEvaluationException {
		final var left  = evaluator.evaluate(arguments.get(0), jsonPath);
		final var right = evaluator.evaluate(arguments.get(1), jsonPath);
		return JSON.equalityValue(left, right);
	}

	private static boolean evaluateBoolean(final String name, final BiPredicate<Double, Double> compare, final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		if(arguments.size() < 2) throw new JsonLogicEvaluationException("'"+name+"' expects 2 or 3 arguments", path);
		// If regular comparisons fail also between will fail
		if(!(evaluator.asDouble(arguments, 0, path) instanceof final Double a)
				|| !(evaluator.asDouble(arguments, 1, path) instanceof final Double b) || !compare.test(a, b)) return false;
		if (arguments.size() == 2) return true; // Handle between comparisons is size = 3
		if(!(evaluator.asDouble(arguments, 2, path) instanceof final Double c)) return false;
		return compare.test(b, c);
	}

	private static boolean has            (final boolean isSome, final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		final var key = isSome ? "some" : "none";
		final var maybeArray = evaluator.evaluate(arguments.get(0), path.sub(0));
		// Array objects can have null values according to http://jsonlogic.com/
		if (maybeArray == null) return !isSome;
		if (!JSON.isList(maybeArray)) throw new JsonLogicEvaluationException("first argument to " + key + " must be a valid array", path.sub(0));
		for (final Object item : JSON.asList(maybeArray)) if(evaluator.scoped(item).asBoolean(arguments.get(1), path.sub(1))) return isSome;
		return !isSome;
	}

	private static Object  andOr          (final boolean isAnd , final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		var index = 0;
		Object value = isAnd;
		for (final var element : arguments) {
			value = evaluator.evaluate(element, path.sub(index++));
			final var result =  JSON.truthy(value);
			if(isAnd ^ result) return value;
		}
		return value;
	}

	private static Object  reduce         (final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		final var maybeArray  = evaluator.evaluate(arguments.get(0), path.sub(0));
		final var accumulator = evaluator.evaluate(arguments.get(2), path.sub(2));
		if (!JSON.isList(maybeArray)) return accumulator;
		final Map<String, Object> context = new HashMap<>();
		context.put("accumulator", accumulator);
		for (final var item : JSON.asList(maybeArray)) {
			context.put("current", item);
			context.put("accumulator", evaluator.scoped(context).evaluate(arguments.get(1), path.sub(1)));
		}
		return context.get("accumulator");
	}

	private static String  substr         (final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		if (!(evaluator.asNumber(arguments, 1, path) instanceof final Number arg1)) throw new JsonLogicEvaluationException("second argument to substr must be a number", path.sub(1));
		final var value = evaluator.evaluate(arguments.get(0), path).toString();
		final var len = value.length();
		if (arguments.size() == 2) {
			var startIndex = arg1.intValue();
			final var endIndex = len;
			if (startIndex < 0) startIndex = endIndex + startIndex;
			if (startIndex < 0) return "";
			return value.substring(startIndex, endIndex);
		}
		if (!(evaluator.asNumber(arguments, 2, path) instanceof final Number arg2)) throw new JsonLogicEvaluationException("third argument to substr must be an integer", path.sub(2));
		var startIndex = arg1.intValue();
		if (startIndex < 0) startIndex = value.length() + startIndex;
		var endIndex = arg2.intValue();
		if (endIndex < 0) endIndex = len + endIndex; else endIndex += startIndex;
		if (startIndex > endIndex || endIndex > value.length()) return "";
		return value.substring(startIndex, endIndex);
	}

	private static List<?> filter         (final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		final var maybeArray = evaluator.evaluate(arguments.get(0), path.sub(0));
		if (!JSON.isList(maybeArray)) throw new JsonLogicEvaluationException("first argument to filter must be a valid array", path.sub(0));
		final var result = new ArrayList<>();
		final var filter = arguments.get(1);
		for (final Object item : JSON.asList(maybeArray)) if(evaluator.scoped(item).asBoolean(filter, path.sub(1))) result.add(item);
		return result;
	}

	private static List<?> map            (final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		final var maybeArray = evaluator.evaluate(arguments.get(0), path.sub(0));
		if (!JSON.isList(maybeArray)) return Collections.emptyList();
		final var list = JSON.asList(maybeArray);
		final var ret = new Object[list.size()];
		var index = 0;
		for (final Object item : list) ret[index++] = evaluator.scoped(item).evaluate(arguments.get(1), path.sub(1));
		return Arrays.asList(ret);
	}

	private static boolean all            (final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		final var maybeArray = evaluator.evaluate(arguments.get(0), path.sub(0));
		if (maybeArray == null) return false;
		if (!JSON.isList(maybeArray)) throw new JsonLogicEvaluationException("first argument to all must be a valid array", path);
		final var array = JSON.asList(maybeArray);
		if (array.size() < 1) return false;
		final var index = 1;
		for (final Object item : array) if(!evaluator.scoped(item).asBoolean(arguments.get(1),  path.sub(index))) return false;
		return true;
	}

	private static boolean in             (final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		if (arguments.size() < 2) return false;
		// Handle string in (substring)
		final var needle   = evaluator.evaluate(arguments.get(0), path.sub(0));
		final var haystack = evaluator.evaluate(arguments.get(1), path.sub(1));
		if (arguments.get(1) instanceof final String t) return (needle == null ? false : t.contains(needle.toString()));
		if(!JSON.isList(haystack)) return false;
		final var l = JSON.asList(haystack);
		for(final var e : l) if(JSON.equalityValue(e, needle)) return true;
		return false;
	}

	private static Object  log            (final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		final var value = evaluator.evaluate(arguments.get(0), path);
		System.out.println("JsonLogic: " + value);
		return value;
	}

	private static Object  ifExpt         (final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		if(arguments.isEmpty()) return null;
		final var size = arguments.size();
		if (size == 1) return evaluator.evaluate(arguments.get(0), path.sub(0));
		for (var i = 0; i < size - 1; i += 2) {
			final var condition    = evaluator.evaluate(arguments.get(i    ), path.sub(i));
			if (evaluator.asBoolean(condition, path.sub(i))) return evaluator.evaluate(arguments.get(i + 1), path.sub(i+1));
		}
		if ((size & 1) == 0) return null;
		return evaluator.evaluate(arguments.get(size - 1), path.sub(size - 1));
	}

	private static List<?> merge          (final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		final var ret = new LinkedList<>();
		final var todo = new LinkedList<Object>(arguments);
		var idx=0;
		while(!todo.isEmpty()) {
			var e = todo.removeFirst();
			e = evaluator.evaluate(e, path.sub(idx++));
			if(JSON.isList(e) && JSON.asList(e) instanceof final List<?> l) {
				for(var i = l.size()-1; i>=0; i--)  todo.add(0, l.get(i));
				continue;
			}
			ret.add(e);
		}
		return ret;
	}

	private static boolean strictEquality (final JsonLogicEvaluator evaluator, final List<?> arguments, final PathSegment path) throws JsonLogicEvaluationException {
		final var left  = evaluator.evaluate(arguments.get(0), path);
		final var right = evaluator.evaluate(arguments.get(1), path);
		if (left instanceof Number && right instanceof Number) return ((Number) left).doubleValue() == ((Number) right).doubleValue();
		if (left == right) return true;
		return left != null && left.equals(right);
	}

	public JsonLogic addOperation    (final String key, final JsonLogicExpressionFI fkt) {
		synchronized (defaultExpressions) {
			if(expressions == defaultExpressions) expressions = new ConcurrentHashMap<>(defaultExpressions);
			expressions.put(key.toLowerCase(), fkt);
		}
		return this;
	}

	public JsonLogic addListOperation(final String name, final Function<List<?>, Object> function) {
		return addOperation(name, (JsonLogicExpressionFI) (evaluator, arguments, jsonPath) -> {
			var values = evaluator.evaluate(arguments, jsonPath);
			if (values.size() == 1 && JSON.isList(values.get(0))) values = JSON.asList(values.get(0));
			return function.apply(arguments);
		});
	}

	public JsonLogic addOperation    (final String name, final Function<Object[], Object> function) {
		return addOperation(name, (JsonLogicExpressionFI) (evaluator, arguments, jsonPath) -> {
			var values = evaluator.evaluate(arguments, jsonPath);
			if (values.size() == 1 && JSON.isList(values.get(0))) values = JSON.asList(values.get(0));
			return function.apply(arguments.toArray());
		});
	}

	/** Parse jsonText to logicExpression */
	private Object   logicExpression(final String jsonTxt) throws JsonLogicException {
		Object exprObj;
		synchronized (parseCache) { exprObj = parseCache.get(jsonTxt); }
		if(null == exprObj) {
			final var jsonObj = JSON.parse(jsonTxt);
			exprObj = JsonLogicParser.parse(jsonObj, PathSegment.ROOT);
			synchronized (parseCache) {
				parseCache.put(jsonTxt, exprObj);
				parseCache.put(jsonObj, exprObj);
			}
		}
		return exprObj;
	}

	/** Parse jsonObject to logicExpression */
	private Object   logicExpression(final Object jsonObj) throws JsonLogicException {
		if(jsonObj instanceof final String json) return logicExpression(json);
		Object exprObj;
		synchronized (parseCache) { exprObj = parseCache.get(jsonObj); }
		if(null == exprObj) {
			exprObj = JsonLogicParser.parse(jsonObj, PathSegment.ROOT);
			synchronized (parseCache) { parseCache.put(jsonObj, exprObj); }
		}
		return exprObj;
	}

	private Object   logicExpressionParsed(final Object jsonObj) throws JsonLogicException {
		Object exprObj;
		synchronized (parseCache) { exprObj = parseCache.get(jsonObj); }
		if(null == exprObj) {
			exprObj = JsonLogicParser.parse(jsonObj, PathSegment.ROOT);
			synchronized (parseCache) { parseCache.put(jsonObj, exprObj); }
		}
		return exprObj;
	}

	public Object    apply           (final Object expr, final Object data) throws JsonLogicException {
		final var dat = data instanceof final String t ? JSON.parse(t) : data;
		return new JsonLogicEvaluator(expressions, number, JSON.plain(dat)).evaluate(logicExpression(expr), PathSegment.ROOT);
	}

	public Object    applyParsed(final Object expr, final Object data) throws JsonLogicException {
		return new JsonLogicEvaluator(expressions, number, JSON.plain(data)).evaluate(logicExpressionParsed(JSON.plain(expr)), PathSegment.ROOT);
	}
}