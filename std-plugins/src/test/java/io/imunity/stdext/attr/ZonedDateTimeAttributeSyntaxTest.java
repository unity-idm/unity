package io.imunity.stdext.attr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.stdext.attr.ZonedDateTimeAttributeSyntax;

class ZonedDateTimeAttributeSyntaxTest
{
	private final ZonedDateTimeAttributeSyntax syntax = new ZonedDateTimeAttributeSyntax();

	@ParameterizedTest
	@MethodSource("dateTimeCases")
	void shouldParseInputAndRoundTripSerializedValue(DateTimeCase scenario)
	{
		ZonedDateTime expected = ZonedDateTime.parse(scenario.expected());

		assertThat(syntax.convertFromString(scenario.input())).usingComparator(ZonedDateTime::compareTo)
				.isEqualTo(expected);
		assertThat(syntax.convertFromString(syntax.convertToString(expected)))
				.usingComparator(ZonedDateTime::compareTo).isEqualTo(expected);
	}

	@Test
	void shouldRejectUnparseableValue()
	{
		assertThatThrownBy(() -> syntax.convertFromString("not-a-date"))
				.isInstanceOf(InternalException.class).hasMessageContaining("Can not parse zoned datetime");
	}

	private static Stream<DateTimeCase> dateTimeCases()
	{
		return Stream.of(
				DateTimeCase.builder().input("2031-01-09T16:45:00Z[UTC]")
						.expected("2031-01-09T16:45:00Z[UTC]").build(),
				DateTimeCase.builder().input("2031-01-09T16:45:00.123456789Z[UTC]")
						.expected("2031-01-09T16:45:00.123456789Z[UTC]").build(),
				DateTimeCase.builder().input("2030-05-08T12:30:00.123+02:00[Europe/Warsaw]")
						.expected("2030-05-08T12:30:00.123+02:00[Europe/Warsaw]").build(),
				DateTimeCase.builder().input("2030-05-08T12:30:00+02:00")
						.expected("2030-05-08T12:30:00+02:00").build(),
				DateTimeCase.builder().input("09-01-2031 16-45-00+01:00[Europe/Warsaw]")
						.expected("2031-01-09T16:45:00+01:00[Europe/Warsaw]").build());
	}

	record DateTimeCase(String input, String expected)
	{
		static Builder builder()
		{
			return new Builder();
		}

		static final class Builder
		{
			private String input;
			private String expected;

			Builder input(String input)
			{
				this.input = input;
				return this;
			}

			Builder expected(String expected)
			{
				this.expected = expected;
				return this;
			}

			DateTimeCase build()
			{
				return new DateTimeCase(input, expected);
			}
		}
	}
}
