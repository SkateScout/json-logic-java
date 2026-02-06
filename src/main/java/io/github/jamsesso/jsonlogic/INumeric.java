package io.github.jamsesso.jsonlogic;

public interface INumeric {
	boolean EQ   (Number a, Number b);
	Number  MINUS(Number a, Number b);
	Number  SUM  (Number a, Number b);
	Number  MUL  (Number a, Number b);
	Number  DIV  (Number a, Number b);
	Number  MOD  (Number a, Number b);
	Number  MIN  (Number a, Number b);
	Number  MAX  (Number a, Number b);
}