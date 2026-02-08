package io.github.jamsesso.jsonlogic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class NumberTests {
	@Test
	public void testConvertAllNumericInputToDouble() throws JsonLogicException {
		final var jsonLogic = new JsonLogic();
		final Map<String, Number> numbers = new HashMap<>() {{
			put("double", 1D);
			put("float", 1F);
			put("int", 1);
			put("short", (short) 1);
			put("long", 1L);
		}};

		assertEquals(1D, jsonLogic.apply("{\"var\": \"double\"}", numbers));
		assertEquals(1D, jsonLogic.apply("{\"var\": \"float\"}", numbers));
		assertEquals(1D, jsonLogic.apply("{\"var\": \"int\"}", numbers));
		assertEquals(1D, jsonLogic.apply("{\"var\": \"short\"}", numbers));
		assertEquals(1D, jsonLogic.apply("{\"var\": \"long\"}", numbers));
	}
}
