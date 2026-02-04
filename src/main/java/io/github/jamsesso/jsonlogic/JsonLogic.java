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
	private JsonLogicEvaluator evaluator;

	private static Object all(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2) throw new JsonLogicEvaluationException("all expects exactly 2 arguments", jsonPath);
		Object maybeArray = evaluator.evaluate(arguments.get(0), data, jsonPath + "[0]");
		if (maybeArray == null) return false;
		if (!ArrayLike.isList(maybeArray)) throw new JsonLogicEvaluationException("first argument to all must be a valid array", jsonPath);
		var array = ArrayLike.asList(maybeArray);
		if (array.size() < 1) return false;
		int index = 1;
		for (Object item : array) if(!evaluator.asBoolean(arguments.get(1), item,  String.format("%s[%d]", jsonPath, index))) return false;
		return true;
	}

	private static Object has(final boolean isSome, final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		var key = isSome ? "some" : "none";
		if (arguments.size() != 2)  throw new JsonLogicEvaluationException(key + " expects exactly 2 arguments", jsonPath);
		Object maybeArray = evaluator.evaluate(arguments.get(0), data, jsonPath + "[0]");

		// Array objects can have null values according to http://jsonlogic.com/
		if (maybeArray == null) return !isSome;
		if (!ArrayLike.isList(maybeArray)) throw new JsonLogicEvaluationException("first argument to " + key + " must be a valid array", jsonPath + "[0]");

		for (Object item : ArrayLike.asList(maybeArray)) if(evaluator.asBoolean(arguments.get(1), item, jsonPath + "[1]")) return isSome;
		return !isSome;
	}

	private static Object andOr(final boolean isAnd, final JsonLogicEvaluator ev, final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() < 1) throw new JsonLogicEvaluationException((isAnd?"and":"or")+" operator expects at least 1 argument", jsonPath);
		int index = 0;
		Object value = isAnd;
		for (var element : arguments) {
			value = ev.evaluate(element, data, String.format("%s[%d]", jsonPath, index++));
			var result =  JsonLogicEvaluator.asBoolean(value);
			if( isAnd && !result) return false;
			if(!isAnd &&  result) return value;
		}
		return value;
	}

	private static  Object in(final JsonLogicEvaluator ev, final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() < 2) return false;
		// Handle string in (substring)
		var needle   = ev.evaluate(arguments.get(0), data, jsonPath+"[0]");
		var haystack = ev.evaluate(arguments.get(1), data, jsonPath+"[1]");
		if (arguments.get(1) instanceof String t) return (needle == null ? false : t.contains(needle.toString()));
		return (ArrayLike.isList(haystack) ? ArrayLike.asList(haystack).contains(needle) : false);
	}

	private static Object log(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.isEmpty()) throw new JsonLogicEvaluationException("log operator requires exactly 1 argument", jsonPath);
		var value = evaluator.evaluate(arguments.get(0), data, jsonPath);
		System.out.println("JsonLogic: " + value);
		return value;
	}

	private static Object evaluate(final String name, final BiPredicate<Double, Double> compare, final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		int n = Math.min(arguments.size(), 3);
		if (n < 2) throw new JsonLogicEvaluationException("'"+name + "' requires at least 2 arguments", jsonPath);
		if (n > 3) throw new JsonLogicEvaluationException("'"+name + "' requires at most 3 arguments", jsonPath);
		// Convert the arguments to doubles
		if(!(evaluator.asDouble(arguments.get(0), data, String.format("%s[0]", jsonPath)) instanceof Double a)) return false;
		if(!(evaluator.asDouble(arguments.get(1), data, String.format("%s[1]", jsonPath)) instanceof Double b)) return false;
		// If regular comparisons fail also between fails
		if(!compare.test(a, b)) return false;
		// Handle between comparisons
		if (arguments.size() == 2) return true;
		if(!(evaluator.asDouble(arguments.get(2), data, String.format("%s[2]", jsonPath)) instanceof Double c)) return false;
		return compare.test(b, c);
	}

	private static Object ifExpt(final JsonLogicEvaluator evaluator, final List<?> args, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if(args.isEmpty()) return null;
		var size = args.size();
		// If there is only a single argument, simply evaluate & return that argument.
		if (size == 1) return evaluator.evaluate(args.get(0), data, jsonPath + "[0]");
		for (int i = 0; i < size - 1; i += 2) {
			var condition    = evaluator.evaluate(args.get(i    ), data, jsonPath + "["+i+"]");
			if (evaluator.asBoolean(condition, data, String.format("%s[%d]", jsonPath, i)))
				return evaluator.evaluate(args.get(i + 1), data, jsonPath + "["+(i+1)+"]");
		}
		if ((size & 1) == 0) return null;
		return evaluator.evaluate(args.get(size - 1), data, String.format("%s[%d]", jsonPath, size - 1));
	}

	private static boolean singleBoolean(final JsonLogicEvaluator evaluator, final List<?> args, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if(args.size()>1) throw new JsonLogicEvaluationException("! expect single argument", jsonPath);
		return evaluator.asBoolean(args.get(0), data, jsonPath);
	}

	public JsonLogic() {
		// Add default operations
		for(var e : Reduce.FUNCTIONS.entrySet()) addOperation(e.getKey(), e.getValue());
		addOperation    ("if" , JsonLogic::ifExpt);	// IF
		addOperation    ("?:" , JsonLogic::ifExpt);	// TERNARY
		addOperation    (">"  , (ev, arg, data, json) -> evaluate(">" , (a,b)->(a >  b), ev, arg, data, json));
		addOperation    (">=" , (ev, arg, data, json) -> evaluate(">=", (a,b)->(a >= b), ev, arg, data, json));
		addOperation    ("<"  , (ev, arg, data, json) -> evaluate("<" , (a,b)->(a <  b), ev, arg, data, json));
		addOperation    ("<=" , (ev, arg, data, json) -> evaluate("<=", (a,b)->(a <= b), ev, arg, data, json));
		addOperation    ("!"  , (ev, arguments, data, jsonPath)-> arguments.isEmpty() ? false : !singleBoolean(ev, arguments, data, jsonPath+"!"));
		addOperation    ("!!" , (ev, arguments, data, jsonPath)-> arguments.isEmpty() ? true  :  singleBoolean(ev, arguments, data, jsonPath+"!!"));
		addOperation    ("and", (ev, arguments, data, jsonPath)-> andOr(true , ev, arguments, data, jsonPath));
		addOperation    ("or" , (ev, arguments, data, jsonPath)-> andOr(false, ev, arguments, data, jsonPath));
		addListOperation("cat", args->args.stream().map(obj -> obj instanceof Double t && t.toString().endsWith(".0") ? t.intValue() : obj).map(Object::toString).collect(Collectors.joining()));
		addOperation    ("==", (ev, arguments, data, jsonPath) ->  EqualityExpression.equality(ev, arguments, data, jsonPath));
		addOperation    ("!=", (ev, arguments, data, jsonPath) -> !EqualityExpression.equality(ev, arguments, data, jsonPath));
		addOperation    (MapExpression.INSTANCE);
		addOperation    (FilterExpression.INSTANCE);
		addOperation    (ReduceExpression.INSTANCE);
		addOperation    ("all" , JsonLogic::all );
		addOperation    ("some", (ev, arguments, data, jsonPath) -> has(true , ev, arguments, data, jsonPath));
		addOperation    ("none", (ev, arguments, data, jsonPath) -> has(false, ev, arguments, data, jsonPath));
		addOperation    ("in", JsonLogic::in);
		addOperation    (SubstringExpression.INSTANCE);
		addOperation    (MissingExpression.ALL);
		addOperation    (MissingExpression.SOME);
		addOperation    ("log", JsonLogic::log);
		addOperation("merge", JsonLogic::merge);
		addOperation("===", JsonLogic::strictEquality);
		addOperation("!==", (ev, arguments, data, jsonPath)->!strictEquality(ev, arguments, data, jsonPath));
	}

	private static List<?> merge(final JsonLogicEvaluator evaluator, final List<?> args, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		var ret = new LinkedList<>();
		var todo = new LinkedList<>();
		todo.addAll(args);
		var idx=0;
		while(!todo.isEmpty()) {
			var e = todo.removeFirst();
			e = evaluator.evaluate(e, data, jsonPath+"["+(idx++)+"]");
			if(ArrayLike.isList(e) && ArrayLike.asList(e) instanceof List l) {
				for(var i = l.size()-1; i>=0; i--)  todo.add(0, l.get(i));
				continue;
			}
			ret.add(e);
		}
		return ret;
	}

	private static boolean strictEquality(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
		if (arguments.size() != 2) throw new JsonLogicEvaluationException("equality expressions expect exactly 2 arguments", jsonPath);
		var left  = evaluator.evaluate(arguments.get(0), data, jsonPath);
		var right = evaluator.evaluate(arguments.get(1), data, jsonPath);
		if (left instanceof Number && right instanceof Number) return ((Number) left).doubleValue() == ((Number) right).doubleValue();
		if (left == right) return true;
		return left != null && left.equals(right);
	}


	public JsonLogic addListOperation(final String name, final Function<List<?>, Object> function) {
		return addOperation(name, new JsonLogicExpressionFI() {
			@Override public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
				List<Object> values = evaluator.evaluate(arguments, data, jsonPath);
				if (values.size() == 1 && ArrayLike.isList(values.get(0))) values = ArrayLike.asList(values.get(0));
				return function.apply(arguments);
			}
		});
	}

	public JsonLogic addOperation(final String name, final Function<Object[], Object> function) {
		return addOperation(name, new JsonLogicExpressionFI() {
			@Override public Object evaluate(final JsonLogicEvaluator evaluator, final List<?> arguments, final Object data, final String jsonPath) throws JsonLogicEvaluationException {
				List<Object> values = evaluator.evaluate(arguments, data, jsonPath);
				if (values.size() == 1 && ArrayLike.isList(values.get(0))) values = ArrayLike.asList(values.get(0));
				return function.apply(arguments.toArray());
			}
		});
	}

	public JsonLogic addOperation(final String key, final JsonLogicExpressionFI fkt) {
		expressions.put(key, fkt);
		evaluator = null;
		return this;
	}

	public JsonLogic addOperation(final JsonLogicExpression expression) { return addOperation(expression.key(), expression); }

	public Object apply(final Object expr, final Object rawData) throws JsonLogicException {
		var data = JSON.plain(rawData);
		if(expr instanceof String t) return apply(t, data);
		if (evaluator == null) evaluator = new JsonLogicEvaluator(expressions);
		return evaluator.evaluate(JsonLogicParser.parse(expr, "$"), data, "$");
	}

	public Object apply(final String json, final Object data) throws JsonLogicException {
		if (!parseCache.containsKey(json)) parseCache.put(json, JsonLogicParser.parse(json));
		if (evaluator == null) evaluator = new JsonLogicEvaluator(expressions);
		return evaluator.evaluate(parseCache.get(json), data, "$");
	}
}