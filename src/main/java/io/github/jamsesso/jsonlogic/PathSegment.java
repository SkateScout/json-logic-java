package io.github.jamsesso.jsonlogic;

public sealed interface PathSegment permits PathSegment.RootSegment, PathSegment.StringSegment, PathSegment.IndexSegment {
	PathSegment ROOT = new RootSegment();

	default String buildString() {
		var length = 1;
		var depth  = 0;
		for(var curr   = this; curr != ROOT; curr = curr.parent()) {
			depth++;
			length += (curr instanceof final StringSegment s ?  s.segment().length() + 1 : 5);
		}
		final var sb        = new StringBuilder(length).append("$");
		final var hierarchy = new PathSegment[depth];

		var curr = this;
		for (var i = depth - 1; i >= 0; i--) {
			hierarchy[i] = curr;
			curr = curr.parent();
		}

		for (final PathSegment segment : hierarchy) {
			switch (segment) {
			case final RootSegment   _ -> sb.append("$");
			case final StringSegment t -> sb.append('.').append(t.segment);
			case final IndexSegment  t -> sb.append('[').append(t.index).append(']');
			}
		}
		return sb.toString();
	}

	record RootSegment() implements PathSegment { @Override public String toString() { return "$"; } }
	record StringSegment(PathSegment parent, String segment) implements PathSegment { @Override public String toString() { return buildString(); } }
	record IndexSegment (PathSegment parent, int    index  ) implements PathSegment { @Override public String toString() { return buildString(); } }

	default PathSegment sub(final String key) { return new StringSegment(this, key); }
	default PathSegment sub(final int key)    { return new IndexSegment(this, key); }
	default PathSegment parent() { return null; }
}