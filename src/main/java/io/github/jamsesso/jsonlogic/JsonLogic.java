package io.github.jamsesso.jsonlogic;

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
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpressionFI;
import io.github.jamsesso.jsonlogic.evaluator.expressions.EqualityExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.FilterExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.MapExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.MissingExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.ReduceExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.SubstringExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

public final class JsonLogic {
	private final Map<String, Object> parseCache = new ConcurrentHashMap<>();
	private final Map<String, JsonLogicExpressionFI> expressions = new ConcurrentHashMap<>();

	private static Object all(final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
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

	private static Object has(final boolean isSome, final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		final var key = isSome ? "some" : "none";
		if (arguments.size() != 2)  throw new JsonLogicEvaluationException(key + " expects exactly 2 arguments", jsonPath);
		final var maybeArray = evaluator.evaluate(arguments.get(0), jsonPath + "[0]");

		// Array objects can have null values according to http://jsonlogic.com/
		if (maybeArray == null) return !isSome;
		if (!ArrayLike.isList(maybeArray)) throw new JsonLogicEvaluationException("first argument to " + key + " must be a valid array", jsonPath + "[0]");
		for (final Object item : ArrayLike.asList(maybeArray)) if(evaluator.scoped(item).asBoolean(arguments.get(1), jsonPath + "[1]")) return isSome;
		return !isSome;
	}

	private static Object andOr(final boolean isAnd, final JsonLogicEvaluator ev, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
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

	private static  Object in(final JsonLogicEvaluator ev, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() < 2) return false;
		// Handle string in (substring)
		final var needle   = ev.evaluate(arguments.get(0), jsonPath+"[0]");
		final var haystack = ev.evaluate(arguments.get(1), jsonPath+"[1]");
		if (arguments.get(1) instanceof final String t) return (needle == null ? false : t.contains(needle.toString()));
		return (ArrayLike.isList(haystack) ? ArrayLike.asList(haystack).contains(needle) : false);
	}

	private static Object log(final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.isEmpty()) throw new JsonLogicEvaluationException("log operator requires exactly 1 argument", jsonPath);
		final var value = evaluator.evaluate(arguments.get(0), jsonPath);
		System.out.println("JsonLogic: " + value);
		return value;
	}

	private static Object evaluate(final String name, final BiPredicate<Double, Double> compare, final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
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

	private static Object ifExpt(final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
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

	private static boolean singleBoolean(final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
		if(args.size()>1) throw new JsonLogicEvaluationException("! expect single argument", jsonPath);
		return evaluator.asBoolean(args.get(0), jsonPath);
	}

	public JsonLogic() {
		// Add default operations
		for(final var e : Reduce.FUNCTIONS.entrySet()) addOperation(e.getKey(), e.getValue());
		addOperation    ("if"  , JsonLogic::ifExpt);	// IF
		addOperation    ("?:"  , JsonLogic::ifExpt);	// TERNARY
		addOperation    (">"   , (ev, arg, json) -> evaluate(">" , (a,b)->(a >  b), ev, arg, json));
		addOperation    (">="  , (ev, arg, json) -> evaluate(">=", (a,b)->(a >= b), ev, arg, json));
		addOperation    ("<"   , (ev, arg, json) -> evaluate("<" , (a,b)->(a <  b), ev, arg, json));
		addOperation    ("<="  , (ev, arg, json) -> evaluate("<=", (a,b)->(a <= b), ev, arg, json));
		addOperation    ("!"   , (ev, arguments, jsonPath)-> arguments.isEmpty() ? false : !singleBoolean(ev, arguments, jsonPath+"!"));
		addOperation    ("!!"  , (ev, arguments, jsonPath)-> arguments.isEmpty() ? true  :  singleBoolean(ev, arguments, jsonPath+"!!"));
		addOperation    ("and" , (ev, arguments, jsonPath)-> andOr(true , ev, arguments, jsonPath));
		addOperation    ("or"  , (ev, arguments, jsonPath)-> andOr(false, ev, arguments, jsonPath));
		addOperation    ("some", (ev, arguments, jsonPath) -> has(true , ev, arguments, jsonPath));
		addOperation    ("none", (ev, arguments, jsonPath) -> has(false, ev, arguments, jsonPath));
		addOperation    ("!="  , (ev, arguments, jsonPath) -> !EqualityExpression.equality(ev, arguments, jsonPath));
		addOperation    ("!==" , (ev, arguments,  jsonPath)->!strictEquality(ev, arguments, jsonPath));

		addListOperation("cat", args->args.stream().map(obj -> obj instanceof final Double t && t.toString().endsWith(".0") ? t.intValue() : obj).map(Object::toString).collect(Collectors.joining()));
		addOperation    ("==", (JsonLogicExpressionFI) EqualityExpression::equality);
		addOperation    (MapExpression.INSTANCE);
		addOperation    (FilterExpression.INSTANCE);
		addOperation    (ReduceExpression.INSTANCE);
		addOperation    ("all" , JsonLogic::all );
		addOperation    ("in", JsonLogic::in);
		addOperation    (SubstringExpression.INSTANCE);
		addOperation    (MissingExpression.ALL);
		addOperation    (MissingExpression.SOME);
		addOperation    ("log", JsonLogic::log);
		addOperation("merge", JsonLogic::merge);
		addOperation("===", JsonLogic::strictEquality);
	}

	private static List<?> merge(final JsonLogicEvaluator evaluator, final List<?> args, final String jsonPath) throws JsonLogicEvaluationException {
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

	private static boolean strictEquality(final JsonLogicEvaluator evaluator, final List<?> arguments, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2) throw new JsonLogicEvaluationException("equality expressions expect exactly 2 arguments", jsonPath);
		final var left  = evaluator.evaluate(arguments.get(0), jsonPath);
		final var right = evaluator.evaluate(arguments.get(1), jsonPath);
		if (left instanceof Number && right instanceof Number) return ((Number) left).doubleValue() == ((Number) right).doubleValue();
		if (left == right) return true;
		return left != null && left.equals(right);
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

	public JsonLogic addOperation(final String key, final JsonLogicExpressionFI fkt) {
		expressions.put(key, fkt);
		return this;
	}

	public JsonLogic addOperation(final JsonLogicExpression expression) { return addOperation(expression.key(), expression); }

	public Object apply(final Object expr, final Object rawData) throws JsonLogicException {
		final var data = JSON.plain(rawData);
		if(expr instanceof final String t) return t;
		final var evaluator = new JsonLogicEvaluator(expressions, data);
		return evaluator.evaluate(JsonLogicParser.parse(expr, "$"), "$");
	}

	public Object apply(final String json) throws JsonLogicException {
		if (!parseCache.containsKey(json)) parseCache.put(json, JsonLogicParser.parse(json));
		final var evaluator = new JsonLogicEvaluator(expressions, null);
		return evaluator.evaluate(parseCache.get(json), "$");
	}
}