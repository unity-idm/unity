package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.ws.rs.core.MediaType;

class UpmanJsonProviderTest
{
	@Test
	void shouldRoundTripProjectWithPublicPropertyAndComputedUrlId() throws Exception
	{
		var mapper = UpmanJsonProvider.newMapper();
		var project = Project.builder().withProjectId("parent/child").withPublic(true)
				.withDisplayedName(Map.of("en", "Child")).build();

		var json = mapper.valueToTree(project);
		var restored = mapper.treeToValue(json, Project.class);

		assertThat(json.properties()).extracting(Map.Entry::getKey).containsExactlyInAnyOrder(
				"projectId", "public", "displayedName", "description", "logoUrl", "enableSubprojects",
				"readOnlyAttributes", "registrationForm", "signUpEnquiry", "membershipUpdateEnquiry", "urlId");
		assertThat(json.get("public").booleanValue()).isTrue();
		assertThat(json.get("urlId").textValue()).isEqualTo("parent%2Fchild");
		assertThat(json.get("readOnlyAttributes").isNull()).isTrue();
		assertThat(restored).isEqualTo(project);
	}

	@Test
	void shouldRoundTripMembershipAndPreserveDefensiveCollectionCopies() throws Exception
	{
		var mapper = UpmanJsonProvider.newMapper();
		var values = new ArrayList<>(List.of("value"));
		var attribute = new MemberAttribute("attribute", values);
		var attributes = new ArrayList<>(List.of(attribute));
		var membership = new ProjectMembership(null, "regular", attributes);

		values.clear();
		attributes.clear();
		var json = mapper.valueToTree(membership);
		var restored = mapper.treeToValue(json, ProjectMembership.class);

		assertThat(json).isEqualTo(mapper.readTree("""
				{"email":null,"role":"regular","attributes":[{"name":"attribute","values":["value"]}]}
				"""));
		assertThat(restored).isEqualTo(membership);
		assertThatThrownBy(() -> restored.attributes().clear()).isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> restored.attributes().getFirst().values().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@ParameterizedTest
	@MethodSource("encodings")
	void shouldDecodeLocalizedNamesUsingTheRequestCharset(Encoding testCase) throws Exception
	{
		String json = """
				{"displayedName":{"en":"café"}}
				""";
		var input = new ByteArrayInputStream(json.getBytes(testCase.charset()));

		var project = (ProjectCreateRequest) new UpmanJsonProvider().read(input, ProjectCreateRequest.class,
				testCase.mediaType());

		assertThat(project.displayedName()).containsEntry("en", "café");
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
