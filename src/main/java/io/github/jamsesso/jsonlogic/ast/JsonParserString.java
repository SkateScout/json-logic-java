package io.github.jamsesso.jsonlogic.ast;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonParserString {
	public static final Object NULL = new Object();
	enum Marker { OBJECT, ARRAY, COMMA, COLON }

	public static Object parse(final String json) {
		if (json == null || json.isBlank()) return null;
		final var len = json.length();
		final Deque<Object> values = new ArrayDeque<>(8192);
		var sizeStack = new int[64];
		int sizePtr = -1, line = 1, column = 0, lastI = 0;

		for (var i = 0; i < len; i++) {
			column += (i - lastI);
			lastI = i;
			final var c = json.charAt(i);
			switch (c) {
			case '\n' -> { line++; column = i + 1; }
			case ' ', '\t', '\r' -> {}
			case 't' -> {
				if (!json.startsWith("true", i)) throw error(line, column, "Invalid 'true'", i);
				values.push(Boolean.TRUE); i += 3;
				if (sizePtr >= 0 && sizeStack[sizePtr] != -1) sizeStack[sizePtr]++;
			}
			case 'f' -> {
				if (!json.startsWith("false", i)) throw error(line, column, "Invalid 'false'", i);
				values.push(Boolean.FALSE); i += 4;
				if (sizePtr >= 0 && sizeStack[sizePtr] != -1) sizeStack[sizePtr]++;
			}
			case 'n' -> {
				if (!json.startsWith("null", i)) throw error(line, column, "Invalid 'null'", i);
				values.push(NULL); i += 3;
				if (sizePtr >= 0 && sizeStack[sizePtr] != -1) sizeStack[sizePtr]++;
			}
			case '"' -> {
				final var start = ++i;
				StringBuilder sb = null;
				while (i < len && json.charAt(i) != '"') {
					if (json.charAt(i) == '\\') {
						if (sb == null) sb = new StringBuilder().append(json, start, i);
						i++;
						final var escaped = json.charAt(i);
						if (escaped == 'u') {
							var val = 0;
							for (var j = 0; j < 4; j++) {
								final var hex = parseHexDigit(json.charAt(++i));
								if (hex == -1) throw error(line, column + (i - lastI), "Invalid hex", i);
								val = (val << 4) | hex;
							}
							sb.append((char) val);
						} else sb.append(handleEscape(escaped));
					} else if (sb != null) sb.append(json.charAt(i));
					i++;
				}
				values.push(sb == null ? json.substring(start, i) : sb.toString());
				if (sizePtr >= 0 && sizeStack[sizePtr] != -1) sizeStack[sizePtr]++;
			}
			case '0','1','2','3','4','5','6','7','8','9','-' -> {
				final var start = i;
				while (i < len && isNumberChar(json.charAt(i))) i++;
				values.push(parseNumber(json, start, i, line, column));
				i--;
				if (sizePtr >= 0 && sizeStack[sizePtr] != -1) sizeStack[sizePtr]++;
			}
			case '{' -> {
				values.push(Marker.OBJECT);
				if (++sizePtr == sizeStack.length) sizeStack = Arrays.copyOf(sizeStack, sizeStack.length * 2);
				sizeStack[sizePtr] = -1;
			}
			case '[' -> {
				values.push(Marker.ARRAY);
				if (++sizePtr == sizeStack.length) sizeStack = Arrays.copyOf(sizeStack, sizeStack.length * 2);
				sizeStack[sizePtr] = 0;
			}
			case '}' -> {
				sizePtr--;
				final Map<String, Object> map = new LinkedHashMap<>();
				var val = values.pop();
				if (val != Marker.OBJECT) {
					do {
						if (Marker.COLON != values.pop()) throw error(line, column, "Expected ':'", i);
						map.put((String) values.pop(), val == NULL ? null : val);
						if ((val = values.pop()) == Marker.OBJECT) break;
						if (Marker.COMMA != val) throw error(line, column, "Expected ','", i);
						val = values.pop();
					} while (true);
				}
				values.push(map);
				if (sizePtr >= 0 && sizeStack[sizePtr] != -1) sizeStack[sizePtr]++;
			}
			case ']' -> {
				final var size = sizeStack[sizePtr--];
				final var arr = new Object[size];
				for (var j = size - 1; j >= 0; j--) {
					final var v = values.pop();
					arr[j] = (v == NULL ? null : v);
					if (j > 0 && Marker.COMMA != values.pop()) throw error(line, column, "Expected ','", i);
				}
				if (Marker.ARRAY != values.pop()) throw error(line, column, "Expected ']'", i);
				values.push(Arrays.asList(arr));
				if (sizePtr >= 0 && sizeStack[sizePtr] != -1) sizeStack[sizePtr]++;
			}

			case ':' -> {
				if (sizePtr < 0 || sizeStack[sizePtr] != -1 || !(values.peek() instanceof String)) throw error(line, column, "Unexpected ':'", i);
				values.push(Marker.COLON);
			}
			case ',' -> {
				if (sizePtr < 0) throw error(line, column, "Unexpected ','", i);
				final var top = values.peek();
				if (top instanceof Marker) throw error(line, column, "Unexpected ','", i);
				values.push(Marker.COMMA);
			}
			default -> throw error(line, column, "Unexpected char", i);
			}
		}
		if (values.size() != 1) throw error(line, column, "Unbalanced", len);
		final var result = values.pop();
		return result == NULL ? null : result;
	}

	private static final double[] POWERS_OF_10 ; static { var v = 1L; POWERS_OF_10 = new double[19]; for(var i=0;i<19;i++) { POWERS_OF_10[i]=v; v*=10; } }
	private static boolean isDigit     (final char c) { return (c >= '0' && c <= '9'); }
	private static boolean isNumberChar(final char c) { return (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E'; }

	public static double parseNumber(final String json, final int start, final int end, final int line, final int col) {
		var mantissa = 0L;
		var exp = 0;
		var i = start;
		final var neg = json.charAt(i) == '-';
		if (neg && ++i >= end) throw error(line, col, "Isolated minus", i);
		if (json.charAt(i) == '0') {
			i++;
			if (i < end && isDigit(json.charAt(i))) throw error(line, col, "Leading zero not allowed", i);
		} else {
			final var s = i;
			while (i < end && isDigit(json.charAt(i))) {
				mantissa = mantissa * 10 + (json.charAt(i++) - '0');
			}
			if (i == s) throw error(line, col, "Expected digit", i);
		}
		if (i < end && json.charAt(i) == '.') {
			i++;
			final var s = i;
			while (i < end && isDigit(json.charAt(i))) {
				mantissa = mantissa * 10 + (json.charAt(i++) - '0');
				exp--;
			}
			if (i == s) throw error(line, col, "Expected digit after .", i);
		}
		if (i < end && (json.charAt(i) == 'e' || json.charAt(i) == 'E')) {
			i++;
			final var eNeg = i < end && json.charAt(i) == '-';
			if (eNeg || (i < end && json.charAt(i) == '+')) i++;
			final var s = i;
			var eVal = 0;
			while (i < end && isDigit(json.charAt(i))) {
				eVal = eVal * 10 + (json.charAt(i++) - '0');
			}
			if (i == s) throw error(line, col, "Expected digit in exponent", i);
			exp += (eNeg ? -eVal : eVal);
		}
		if (i != end) throw error(line, col, "Invalid number syntax", i);
		final var absExp = Math.abs(exp);
		final var res = (absExp < POWERS_OF_10.length ? (exp >= 0 ? mantissa * POWERS_OF_10[absExp] : mantissa / POWERS_OF_10[absExp]) : mantissa * Math.pow(10, exp));
		return neg ? -res : res;
	}

	private static int parseHexDigit(final char c) {
		if (c >= '0' && c <= '9') return c - '0';
		if (c >= 'a' && c <= 'f') return c - 'a' + 10;
		if (c >= 'A' && c <= 'F') return c - 'A' + 10;
		return -1;
	}

	private static char handleEscape(final char c) {
		return switch (c) {
		case '\\' -> '\\'; case '/' -> '/'; case '"' -> '"';
		case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t';
		case 'b' -> '\b'; case 'f' -> '\f'; default -> c;
		};
	}

	private static IllegalStateException error(final int line, final int col, final String msg, final int i) {
		return new IllegalStateException(String.format("[%d:%d] %s at index %d", line, col, msg, i));
	}
}