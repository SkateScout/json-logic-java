package io.github.jamsesso.jsonlogic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.github.jamsesso.jsonlogic.utils.JsonValueExtractor;

public class FixtureTests {
	private static final List<Fixture> FIXTURES = readFixtures("fixtures.json", Fixture::fromArray);

	public static <F> List<F> readFixtures(final String fileName, final Function<JsonArray, F> makeFixture) {
		final var inputStream = ErrorFixtureTests.class.getClassLoader().getResourceAsStream(fileName);
		final var parser = new JsonParser();
		final var json = parser.parse(new InputStreamReader(inputStream)).getAsJsonArray();

		final List<F> fixtures = new ArrayList<>();
		// Pull out each fixture from the array.
		for (final JsonElement element : json) {
			if (!element.isJsonArray()) {
				continue;
			}

			final var array = element.getAsJsonArray();
			fixtures.add(makeFixture.apply(array));
		}
		return fixtures;
	}

	@Test
	public void testAllFixtures() {
		final var jsonLogic = new JsonLogic();
		final List<TestResult> failures = new ArrayList<>();

		for (final Fixture fixture : FIXTURES) {
			try {
				final var result = jsonLogic.apply(fixture.getJson(), fixture.getData());

				if (!Objects.equals(result, fixture.getExpectedValue())) {
					failures.add(new TestResult(fixture, result));
				}
			}
			catch (final JsonLogicException e) {
				failures.add(new TestResult(fixture, e));
			}
		}

		for (final TestResult testResult : failures) {
			final var actual  = testResult.getResult();
			final var fixture = testResult.getFixture();
			final var expectedValue = fixture.getExpectedValue();


			System.out.println("actual       : {"+actual.getClass().getCanonicalName()+"}"+actual);
			System.out.println("json         : "+fixture.getJson());
			System.out.println("data         : "+fixture.getData());
			System.out.println("expectedValue: {"+(null==expectedValue?null:expectedValue.getClass().getCanonicalName())+"}"+expectedValue);

			System.out.println(String.format("FAIL: %s\n\t%s\n\tExpected: %s Got: %s\n", fixture.getJson(), fixture.getData(),
					fixture.getExpectedValue(), actual instanceof final Exception e ? e.getMessage() : actual));
		}

		assertEquals(0, failures.size(), String.format("%d/%d test failures!", failures.size(), FIXTURES.size()));
	}

	private static class Fixture {
		public static Fixture fromArray(final JsonArray array) {
			return new Fixture(array.get(0).toString(), array.get(1), array.get(2));
		}

		private final String json;
		private final Object data;
		private final Object expectedValue;

		private Fixture(final String json, final JsonElement data, final JsonElement expectedValue) {
			this.json = json;
			this.data = JsonValueExtractor.extract(data);
			this.expectedValue = JsonValueExtractor.extract(expectedValue);
		}

		String getJson() {
			return json;
		}

		Object getData() {
			return data;
		}

		Object getExpectedValue() {
			return expectedValue;
		}
	}

	private static class TestResult {
		private final Fixture fixture;
		private final Object result;

		private TestResult(final Fixture fixture, final Object result) {
			this.fixture = fixture;
			this.result = result;
		}

		Fixture getFixture() {
			return fixture;
		}

		Object getResult() {
			return result;
		}
	}
}
