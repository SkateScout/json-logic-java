package io.github.jamsesso.jsonlogic;

public enum NumericDouble implements INumeric {
	ONCE;

	@Override public boolean EQ   (final Number a, final Number b) { return (a!=null && b!=null && a.doubleValue()==b.doubleValue()); }
	@Override public Number  MINUS(final Number a, final Number b) { return                        a.doubleValue()- b.doubleValue() ; }
	@Override public Number  SUM  (final Number a, final Number b) { return                        a.doubleValue()+ b.doubleValue() ; }
	@Override public Number  MUL  (final Number a, final Number b) { return                        a.doubleValue()* b.doubleValue() ; }
	@Override public Number  DIV  (final Number a, final Number b) { return                        a.doubleValue()/ b.doubleValue() ; }
	@Override public Number  MOD  (final Number a, final Number b) { return                        a.doubleValue()% b.doubleValue() ; }
	@Override public Number  MAX  (final Number a, final Number b) { return Double.max            (a.doubleValue(),b.doubleValue()) ; }
	@Override public Number  MIN  (final Number a, final Number b) { return Double.min            (a.doubleValue(),b.doubleValue()) ; }
}