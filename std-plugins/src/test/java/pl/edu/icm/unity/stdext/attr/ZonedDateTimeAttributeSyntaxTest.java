/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pl.edu.icm.unity.base.exceptions.InternalException;

class ZonedDateTimeAttributeSyntaxTest
{
	private final ZonedDateTimeAttributeSyntax syntax = new ZonedDateTimeAttributeSyntax();

	@ParameterizedTest
	@MethodSource("serializedValues")
	void shouldParseValueProducedBySerializer(ZonedDateTime value)
	{
		// given
		String serialized = syntax.convertToString(value);

		// when
		ZonedDateTime parsed = syntax.convertFromString(serialized);

		// then
		assertThat(parsed).isEqualTo(value);
	}

	static Stream<ZonedDateTime> serializedValues()
	{
		return Stream.of(
				ZonedDateTime.of(2030, 5, 8, 12, 30, 0, 0, ZoneId.of("Europe/Warsaw")),
				ZonedDateTime.of(2031, 1, 9, 16, 45, 0, 0, ZoneId.of("UTC")),
				ZonedDateTime.of(2031, 1, 9, 16, 45, 0, 0, ZoneOffset.UTC),
				ZonedDateTime.of(2031, 1, 9, 16, 45, 0, 123_000_000, ZoneId.of("Europe/Warsaw")),
				ZonedDateTime.of(2031, 1, 9, 16, 45, 0, 123_456_789, ZoneId.of("UTC")));
	}

	@ParameterizedTest
	@MethodSource("textualValues")
	void shouldParseSupportedTextualFormats(ParsingCase parsingCase)
	{
		// when
		ZonedDateTime parsed = syntax.convertFromString(parsingCase.input());

		// then
		assertThat(parsed).isEqualTo(parsingCase.expected());
	}

	static Stream<ParsingCase> textualValues()
	{
		LocalDateTime dateTime = LocalDateTime.of(2031, 1, 9, 16, 45);
		return Stream.of(
				ParsingCase.builder()
						.withInput("2031-01-09T16:45:00Z[UTC]")
						.withExpected(ZonedDateTime.of(dateTime, ZoneId.of("UTC")))
						.build(),
				ParsingCase.builder()
						.withInput("2031-01-09T16:45:00Z")
						.withExpected(ZonedDateTime.of(dateTime, ZoneOffset.UTC))
						.build(),
				ParsingCase.builder()
						.withInput("2031-01-09 16:45:00Z")
						.withExpected(ZonedDateTime.of(dateTime, ZoneOffset.UTC))
						.build(),
				ParsingCase.builder()
						.withInput("09.01.2031 16.45.00+01:00")
						.withExpected(ZonedDateTime.of(dateTime, ZoneOffset.ofHours(1)))
						.build(),
				ParsingCase.builder()
						.withInput("09.01.2031 16.45.00+01:00[Europe/Warsaw]")
						.withExpected(ZonedDateTime.of(dateTime, ZoneId.of("Europe/Warsaw")))
						.build());
	}

	@Test
	void shouldRejectUnparsableValue()
	{
		assertThatThrownBy(() -> syntax.convertFromString("not a date"))
				.isInstanceOf(InternalException.class)
				.hasMessageContaining("Can not parse zoned datetime");
	}

	record ParsingCase(String input, ZonedDateTime expected)
	{
		static Builder builder()
		{
			return new Builder();
		}

		static final class Builder
		{
			private String input;
			private ZonedDateTime expected;

			Builder withInput(String input)
			{
				this.input = input;
				return this;
			}

			Builder withExpected(ZonedDateTime expected)
			{
				this.expected = expected;
				return this;
			}

			ParsingCase build()
			{
				return new ParsingCase(input, expected);
			}
		}
	}
}
