package io.github.jamsesso.jsonlogic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.jamsesso.jsonlogic.ast.JSON;

public class TruthyTests {
	@Test
	public void testTruthyValues() {
		// Zero
		assertFalse(JSON.truthy(0));

		// Any non-zero number
		assertTrue(JSON.truthy(1.04));
		assertTrue(JSON.truthy(-1));

		// Empty array or collection
		assertFalse(JSON.truthy(Collections.EMPTY_LIST));
		assertFalse(JSON.truthy(new int[0]));

		// Any non-empty array or collection
		assertTrue(JSON.truthy(Collections.singleton(1)));
		assertTrue(JSON.truthy(new boolean[] {false}));

		// Empty string
		assertFalse(JSON.truthy(""));

		// Any non-empty string
		assertTrue(JSON.truthy("hello world"));
		assertTrue(JSON.truthy("0"));

		// Null
		assertFalse(JSON.truthy(null));

		// NaN and Infinity
		assertFalse(JSON.truthy(Double.NaN));
		assertFalse(JSON.truthy(Float.NaN));
		assertTrue(JSON.truthy(Double.POSITIVE_INFINITY));
		assertTrue(JSON.truthy(Double.NEGATIVE_INFINITY));
		assertTrue(JSON.truthy(Float.POSITIVE_INFINITY));
		assertTrue(JSON.truthy(Float.NEGATIVE_INFINITY));
	}
}
