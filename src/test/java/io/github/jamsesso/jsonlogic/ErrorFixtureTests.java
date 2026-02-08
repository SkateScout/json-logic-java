package io.github.jamsesso.jsonlogic;

import static io.github.jamsesso.jsonlogic.FixtureTests.readFixtures;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.github.jamsesso.jsonlogic.utils.JsonValueExtractor;

public class ErrorFixtureTests {
	private static final List<ErrorFixture> FIXTURES = readFixtures("error-fixtures.json", ErrorFixture::fromArray);

	@Test
	public void testAllFixtures() {
		final var jsonLogic = new JsonLogic();
		final List<TestResult> failures = new ArrayList<>();

		for (final ErrorFixture fixture : FIXTURES) {
			try {
				jsonLogic.apply(fixture.getJson(), fixture.getData());
				failures.add(new TestResult(fixture, new JsonLogicException("Expected an exception at " + fixture.getExpectedJsonPath(), PathSegment.ROOT)));
			} catch (final JsonLogicException e) {
				if (!fixture.getExpectedJsonPath().equals(e.getJsonPath()) ||
						!fixture.getExpectedError().equals(e.getMessage())) {
					failures.add(new TestResult(fixture, e));
				}
			}
		}

		for (final TestResult testResult : failures) {
			final var exception = testResult.getException();
			final var fixture = testResult.getFixture();

			System.out.printf("FAIL: %s\n\t%s\n\tExpected: %s at %s Got: \"%s\" at \"%s\"\n%n", fixture.getJson(), fixture.getData(),
					fixture.getExpectedError(), fixture.getExpectedJsonPath(),
					exception.getMessage(), exception.getJsonPath());
		}

		assertEquals(0, failures.size(), String.format("%d/%d test failures!", failures.size(), FIXTURES.size()));
	}

	private static class ErrorFixture {
		private final String json;
		private final Object data;
		private final String expectedPath;
		private final String expectedError;

		private ErrorFixture(final String json, final JsonElement data, final String expectedPath, final String expectedError) {
			this.json = json;
			this.data = JsonValueExtractor.extract(data);
			this.expectedPath = expectedPath;
			this.expectedError = expectedError;
		}

		public static ErrorFixture fromArray(final JsonArray array) {
			return new ErrorFixture(array.get(0).toString(), array.get(1), array.get(2).getAsString(), array.get(3).getAsString());
		}

		String getJson() {
			return json;
		}

		Object getData() {
			return data;
		}

		String getExpectedJsonPath() {
			return expectedPath;
		}

		String getExpectedError() {
			return expectedError;
		}
	}

	private static class TestResult {
		private final ErrorFixture fixture;
		private final JsonLogicException exception;

		private TestResult(final ErrorFixture fixture, final JsonLogicException exception) {
			this.fixture = fixture;
			this.exception = exception;
		}

		ErrorFixture getFixture() {
			return fixture;
		}

		JsonLogicException getException() {
			return exception;
		}
	}
}
