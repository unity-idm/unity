package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.ws.rs.core.MediaType;

class UpmanJsonProviderTest
{
	@ParameterizedTest
	@MethodSource("encodings")
	void shouldDecodeLocalizedNamesUsingTheRequestCharset(Encoding testCase) throws Exception
	{
		String json = """
				{"displayedName":{"en":"café"}}
				""";
		var input = new ByteArrayInputStream(json.getBytes(testCase.charset()));

		var project = (RestProjectCreateRequest) new UpmanJsonProvider().read(input, RestProjectCreateRequest.class,
				testCase.mediaType());

		assertThat(project.displayedName).containsEntry("en", "café");
	}

	private static Stream<Encoding> encodings()
	{
		return Stream.of(
				Encoding.builder().mediaType("application/json").build(),
				Encoding.builder().mediaType("application/json; charset=UTF-8").build(),
				Encoding.builder().mediaType("text/plain; charset=ISO-8859-1")
						.charset(StandardCharsets.ISO_8859_1).build());
	}

	private record Encoding(MediaType mediaType, Charset charset)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private MediaType mediaType;
			private Charset charset = StandardCharsets.UTF_8;

			Builder mediaType(String mediaType)
			{
				this.mediaType = MediaType.valueOf(mediaType);
				return this;
			}

			Builder charset(Charset charset)
			{
				this.charset = charset;
				return this;
			}

			Encoding build()
			{
				return new Encoding(mediaType, charset);
			}
		}
	}
}
