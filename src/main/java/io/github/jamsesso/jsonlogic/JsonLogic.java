package io.github.jamsesso.jsonlogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.jamsesso.jsonlogic.ast.JSON;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpressionFI;
import io.github.jamsesso.jsonlogic.evaluator.expressions.EqualityExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.MissingExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public final class JsonLogic {
	private final Map<String, Object               > parseCache  = new ConcurrentHashMap<>();
	private       Map<String, JsonLogicExpressionFI> expressions;

	private static void addOperation(final Map<String, JsonLogicExpressionFI> e, final String key, final JsonLogicExpressionFI fkt) { e.put(key, fkt); }

	private static void addListOperation(final Map<String, JsonLogicExpressionFI> e, final String name, final Function<List<?>, Object> function) {
		addOperation(e, name, (JsonLogicExpressionFI) (evaluator, arguments, jsonPath) -> {
			var values = evaluator.evaluate(arguments, jsonPath);
			if (values.size() == 1 && ArrayLike.isList(values.get(0))) values = ArrayLike.asList(values.get(0));
			return function.apply(arguments);
		});
	}

	private static final Map<String, JsonLogicExpressionFI> defaultExpressions;

	static {
		final var m = new HashMap<String, JsonLogicExpressionFI>();
		addOperation    (m, "if"          , JsonLogic::ifExpt);	// IF
		addOperation    (m, "?:"          , JsonLogic::ifExpt);	// TERNARY
		addOperation    (m, ">"           , (ev, arg, json) -> evaluateBoolean(">" , (a,b)->(a >  b), ev, arg, json));
		addOperation    (m, ">="          , (ev, arg, json) -> evaluateBoolean(">=", (a,b)->(a >= b), ev, arg, json));
		addOperation    (m, "<"           , (ev, arg, json) -> evaluateBoolean("<" , (a,b)->(a <  b), ev, arg, json));
		addOperation    (m, "<="          , (ev, arg, json) -> evaluateBoolean("<=", (a,b)->(a <= b), ev, arg, json));
		addOperation    (m, "!"           , (ev, arguments, jsonPath) -> arguments.isEmpty() ? false : !singleBoolean(ev, arguments, jsonPath+"!"));
		addOperation    (m, "!!"          , (ev, arguments, jsonPath) -> arguments.isEmpty() ? true  :  singleBoolean(ev, arguments, jsonPath+"!!"));
		addOperation    (m, "and"         , (ev, arguments, jsonPath) -> andOr(true , ev, arguments, jsonPath));
		addOperation    (m, "or"          , (ev, arguments, jsonPath) -> andOr(false, ev, arguments, jsonPath));
		addOperation    (m, "some"        , (ev, arguments, jsonPath) -> has  (true , ev, arguments, jsonPath));
		addOperation    (m, "none"        , (ev, arguments, jsonPath) -> has  (false, ev, arguments, jsonPath));
		addOperation    (m, "!="          , (ev, arguments, jsonPath) -> !EqualityExpression.equality(ev, arguments, jsonPath));
		addOperation    (m, "!=="         , (ev, arguments,  jsonPath)->!strictEquality(ev, arguments, jsonPath));
		addOperation    (m, "==="         , JsonLogic::strictEquality);
		addOperation    (m, "=="          , (JsonLogicExpressionFI) EqualityExpression::equality);
		addOperation    (m, "map"         , JsonLogic       ::map);
		addOperation    (m, "filter"      , JsonLogic       ::filter);
		addOperation    (m, "reduce"      , JsonLogic       ::reduce);
		addOperation    (m, "all"         , JsonLogic       ::all );
		addOperation    (m, "in"          , JsonLogic       ::in);
		addOperation    (m, "substr"      , JsonLogic       ::substr);
		addOperation    (m, "-"           , new Reduce(Reduce.R_MINUS , 0, 1));
		addOperation    (m, "+"           , new Reduce(Double::sum    , 0, 1));
		addOperation    (m, "*"           , new Reduce((a, b) -> a * b, 0, 1));
		addOperation    (m, "/"           , new Reduce((a, b) -> a / b, 2, 2));
		addOperation    (m, "%"           , new Reduce((a, b) -> a % b, 2, 2));
		addOperation    (m, "min"         , new Reduce(Math::min      , 0, 1));
		addOperation    (m, "max"         , new Reduce(Math::max      , 0, 1));
		addListOperation(m, "cat"         , a->a.stream().map(o -> o instanceof final Double t && t.toString().endsWith(".0") ? t.intValue() : o).map(Object::toString).collect(Collectors.joining()));
		addOperation    (m, "missing"     , new MissingExpression(false));
		addOperation    (m, "missing_some", new MissingExpression(true ));
		addOperation    (m, "log"         , JsonLogic::log);
		addOperation    (m, "merge"       , JsonLogic::merge);
		defaultExpressions = m;
	}

	private static boolean evaluateBoolean(final String name, final BiPredicate<Double, Double> compare, final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		final var n = Math.min(arguments.size(), 3);
		if (n < 2) throw new JsonLogicEvaluationException("'"+name + "' requires at least 2 arguments", jsonPath);
		if (n > 3) throw new JsonLogicEvaluationException("'"+name + "' requires at most 3 arguments", jsonPath);
		// Convert the arguments to doubles
		// If regular comparisons fail also between fails
		if(!(evaluator.asDouble(arguments.get(0), String.format("%s[0]", jsonPath)) instanceof final Double a) || !(evaluator.asDouble(arguments.get(1), String.format("%s[1]", jsonPath)) instanceof final Double b) || !compare.test(a, b)) return false;
		// Handle between comparisons
		if (arguments.size() == 2) return true;
		if(!(evaluator.asDouble(arguments.get(2), String.format("%s[2]", jsonPath)) instanceof final Double c)) return false;
		return compare.test(b, c);
	}

	private static boolean has            (final boolean isSome, final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		final var key = isSome ? "some" : "none";
		if (arguments.size() != 2)  throw new JsonLogicEvaluationException(key + " expects exactly 2 arguments", jsonPath);
		final var maybeArray = evaluator.evaluate(arguments.get(0), jsonPath + "[0]");

		// Array objects can have null values according to http://jsonlogic.com/
		if (maybeArray == null) return !isSome;
		if (!ArrayLike.isList(maybeArray)) throw new JsonLogicEvaluationException("first argument to " + key + " must be a valid array", jsonPath + "[0]");
		for (final Object item : ArrayLike.asList(maybeArray)) if(evaluator.scoped(item).asBoolean(arguments.get(1), jsonPath + "[1]")) return isSome;
		return !isSome;
	}

	private static Object  andOr          (final boolean isAnd, final JsonLogicEvaluator ev, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() < 1) throw new JsonLogicEvaluationException((isAnd?"and":"or")+" operator expects at least 1 argument", jsonPath);
		var index = 0;
		Object value = isAnd;
		for (final var element : arguments) {
			value = ev.evaluate(element, String.format("%s[%d]", jsonPath, index++));
			final var result =  JsonLogicEvaluator.asBoolean(value);
			if( isAnd && !result) return false;
			if(!isAnd &&  result) return value;
		}
		return value;
	}

	private static Object  reduce         (final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 3) throw new JsonLogicEvaluationException("reduce expects exactly 3 arguments", jsonPath);
		final var maybeArray  = evaluator.evaluate(arguments.get(0), jsonPath + "[0]");
		final var accumulator = evaluator.evaluate(arguments.get(2), jsonPath + "[2]");
		if (!ArrayLike.isList(maybeArray)) return accumulator;
		final Map<String, Object> context = new HashMap<>();
		context.put("accumulator", accumulator);
		for (final var item : ArrayLike.asList(maybeArray)) {
			context.put("current", item);
			context.put("accumulator", evaluator.scoped(context).evaluate(arguments.get(1), jsonPath + "[1]"));
		}
		return context.get("accumulator");
	}

	private static String  substr         (final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() < 2 || arguments.size() > 3) throw new JsonLogicEvaluationException("substr expects 2 or 3 arguments", jsonPath);
		if (!(evaluator.asDouble(arguments.get(1), jsonPath) instanceof final Double arg1)) throw new JsonLogicEvaluationException("second argument to substr must be a number", jsonPath + "[1]");
		final var value = evaluator.evaluate(arguments.get(0), jsonPath).toString();
		final var len = value.length();
		if (arguments.size() == 2) {
			var startIndex = arg1.intValue();
			final var endIndex = len;
			if (startIndex < 0) startIndex = endIndex + startIndex;
			if (startIndex < 0) return "";
			return value.substring(startIndex, endIndex);
		}
		if (!(evaluator.asDouble(arguments.get(2), jsonPath) instanceof final Double arg2)) throw new JsonLogicEvaluationException("third argument to substr must be an integer", jsonPath + "[2]");
		var startIndex = arg1.intValue();
		if (startIndex < 0) startIndex = value.length() + startIndex;
		var endIndex = arg2.intValue();
		if (endIndex < 0) endIndex = len + endIndex; else endIndex += startIndex;
		if (startIndex > endIndex || endIndex > value.length()) return "";
		return value.substring(startIndex, endIndex);
	}

	private static List<?> filter         (final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2) throw new JsonLogicEvaluationException("filter expects exactly 2 arguments", jsonPath);
		final var maybeArray = evaluator.evaluate(arguments.get(0), jsonPath + "[0]");
		if (!ArrayLike.isList(maybeArray)) throw new JsonLogicEvaluationException("first argument to filter must be a valid array", jsonPath + "[0]");
		final var result = new ArrayList<>();
		final var filter = arguments.get(1);
		for (final Object item : ArrayLike.asList(maybeArray)) if(evaluator.scoped(item).asBoolean(filter, jsonPath + "[1]")) result.add(item);
		return result;
	}

	private static List<?> map            (final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2)  throw new JsonLogicEvaluationException("map expects exactly 2 arguments", jsonPath);
		final var maybeArray = evaluator.evaluate(arguments.get(0), jsonPath + "[0]");
		if (!ArrayLike.isList(maybeArray)) return Collections.emptyList();
		final var list = ArrayLike.asList(maybeArray);
		final var ret = new Object[list.size()];
		var index = 0;
		for (final Object item : list) ret[index++] = evaluator.scoped(item).evaluate(arguments.get(1), jsonPath + "[1]");
		return Arrays.asList(ret);
	}

	private static boolean all            (final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2) throw new JsonLogicEvaluationException("all expects exactly 2 arguments", jsonPath);
		final var maybeArray = evaluator.evaluate(arguments.get(0), jsonPath + "[0]");
		if (maybeArray == null) return false;
		if (!ArrayLike.isList(maybeArray)) throw new JsonLogicEvaluationException("first argument to all must be a valid array", jsonPath);
		final var array = ArrayLike.asList(maybeArray);
		if (array.size() < 1) return false;
		final var index = 1;
		for (final Object item : array) if(!evaluator.scoped(item).asBoolean(arguments.get(1),  String.format("%s[%d]", jsonPath, index))) return false;
		return true;
	}

	private static boolean in             (final JsonLogicEvaluator ev, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() < 2) return false;
		// Handle string in (substring)
		final var needle   = ev.evaluate(arguments.get(0), jsonPath+"[0]");
		final var haystack = ev.evaluate(arguments.get(1), jsonPath+"[1]");
		if (arguments.get(1) instanceof final String t) return (needle == null ? false : t.contains(needle.toString()));
		return (ArrayLike.isList(haystack) ? ArrayLike.asList(haystack).contains(needle) : false);
	}

	private static Object  log            (final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.isEmpty()) throw new JsonLogicEvaluationException("log operator requires exactly 1 argument", jsonPath);
		final var value = evaluator.evaluate(arguments.get(0), jsonPath);
		System.out.println("JsonLogic: " + value);
		return value;
	}

	private static Object  ifExpt         (final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
		if(args.isEmpty()) return null;
		final var size = args.size();
		// If there is only a single argument, simply evaluate & return that argument.
		if (size == 1) return evaluator.evaluate(args.get(0), jsonPath + "[0]");
		for (var i = 0; i < size - 1; i += 2) {
			final var condition    = evaluator.evaluate(args.get(i    ), jsonPath + "["+i+"]");
			if (evaluator.asBoolean(condition, String.format("%s[%d]", jsonPath, i)))
				return evaluator.evaluate(args.get(i + 1), jsonPath + "["+(i+1)+"]");
		}
		if ((size & 1) == 0) return null;
		return evaluator.evaluate(args.get(size - 1), String.format("%s[%d]", jsonPath, size - 1));
	}

	private static boolean singleBoolean  (final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
		if(args.size()>1) throw new JsonLogicEvaluationException("! expect single argument", jsonPath);
		return evaluator.asBoolean(args.get(0), jsonPath);
	}

	private static List<?> merge          (final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
		final var ret = new LinkedList<>();
		final var todo = new LinkedList<Object>(args);
		var idx=0;
		while(!todo.isEmpty()) {
			var e = todo.removeFirst();
			e = evaluator.evaluate(e, jsonPath+"["+(idx++)+"]");
			if(ArrayLike.isList(e) && ArrayLike.asList(e) instanceof final List<?> l) {
				for(var i = l.size()-1; i>=0; i--)  todo.add(0, l.get(i));
				continue;
			}
			ret.add(e);
		}
		return ret;
	}

	private static boolean strictEquality (final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2) throw new JsonLogicEvaluationException("equality expressions expect exactly 2 arguments", jsonPath);
		final var left  = evaluator.evaluate(arguments.get(0), jsonPath);
		final var right = evaluator.evaluate(arguments.get(1), jsonPath);
		if (left instanceof Number && right instanceof Number) return ((Number) left).doubleValue() == ((Number) right).doubleValue();
		if (left == right) return true;
		return left != null && left.equals(right);
	}

	public JsonLogic() { expressions = defaultExpressions; }

	public JsonLogic addOperation(final String key, final JsonLogicExpressionFI fkt) {
		synchronized (defaultExpressions) {
			if(expressions == defaultExpressions) expressions = new ConcurrentHashMap<>(defaultExpressions);
			expressions.put(key, fkt);
		}
		return this;
	}

	public JsonLogic addListOperation(final String name, final Function<List<?>, Object> function) {
		return addOperation(name, (JsonLogicExpressionFI) (evaluator, arguments, jsonPath) -> {
			var values = evaluator.evaluate(arguments, jsonPath);
			if (values.size() == 1 && ArrayLike.isList(values.get(0))) values = ArrayLike.asList(values.get(0));
			return function.apply(arguments);
		});
	}

	public JsonLogic addOperation(final String name, final Function<Object[], Object> function) {
		return addOperation(name, (JsonLogicExpressionFI) (evaluator, arguments, jsonPath) -> {
			var values = evaluator.evaluate(arguments, jsonPath);
			if (values.size() == 1 && ArrayLike.isList(values.get(0))) values = ArrayLike.asList(values.get(0));
			return function.apply(arguments.toArray());
		});
	}

	public Object apply(final Object expr, final Object rawData) throws JsonLogicException {
		if(expr instanceof final String t) return t;
		final var evaluator = new JsonLogicEvaluator(expressions, JSON.plain(rawData));
		final var expression = JsonLogicParser.parse(expr, "$");
		return    evaluator.evaluate(expression, "$");
	}

	public Object apply(final String json) throws JsonLogicException {
		if (!parseCache.containsKey(json)) parseCache.put(json, JsonLogicParser.parse(json));
		final var evaluator = new JsonLogicEvaluator(expressions, null);
		return    evaluator.evaluate(parseCache.get(json), "$");
	}
}