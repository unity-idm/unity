package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class UpmanProjectHttpContractTest extends UpmanHttpContractTestBase
{
	private static final String PROJECTS = "/restupm/v1/projects";
	private static final String PROJECT = PROJECTS + "/contract";

	@Test
	void shouldPreserveCompleteProjectTreesAcrossCrud() throws Exception
	{
		String create = fixture("project-create.json");
		JsonNode expected = m.readTree(fixture("project.json"));

		assertJson(exchange("POST", PROJECTS, create, ContentType.APPLICATION_JSON), 200, "{\"id\":\"contract\"}");
		assertJson(exchange("GET", PROJECT), 200, expected);
		assertJson(exchange("GET", PROJECTS), 200, m.createArrayNode().add(expected));
		assertEmpty(exchange("PUT", PROJECT, fixture("project-update.json"), ContentType.APPLICATION_JSON), 204);
		assertJson(exchange("GET", PROJECT), 200, fixture("project-updated.json"));
		assertEmpty(exchange("DELETE", PROJECT), 204);
		assertJson(exchange("GET", PROJECTS), 200, "[]");
		assertThat(exchange("GET", PROJECT).status()).isEqualTo(404);
	}

	@Test
	void shouldGenerateAnIdWhenOmitted() throws Exception
	{
		ObjectNode create = (ObjectNode) m.readTree(fixture("project-create.json"));
		create.remove("projectId");

		HttpResult result = exchange("POST", PROJECTS, create.toString(), ContentType.APPLICATION_JSON);

		assertThat(result.status()).isEqualTo(200);
		assertThat(result.contentType()).startsWith("application/json");
		JsonNode id = m.readTree(result.body());
		assertThat(id.properties()).hasSize(1);
		assertThat(id.path("id").asText()).hasSize(5);
		ObjectNode expected = (ObjectNode) m.readTree(fixture("project.json"));
		expected.put("projectId", id.get("id").asText());
		expected.put("urlId", id.get("id").asText());
		assertJson(exchange("GET", PROJECTS + "/" + id.get("id").asText()), 200, expected);
	}

	@Test
	void shouldAcceptLegacyMediaTypesAndAbsentContentType() throws Exception
	{
		for (ContentType contentType : new ContentType[] {ContentType.APPLICATION_JSON,
				ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8), null})
		{
			assertJson(exchange("POST", PROJECTS, fixture("project-create.json"), contentType),
					200, "{\"id\":\"contract\"}");
			assertEmpty(exchange("PUT", PROJECT, fixture("project-update.json"), contentType), 204);
			assertJson(exchange("GET", PROJECT), 200, fixture("project-updated.json"));
			assertEmpty(exchange("DELETE", PROJECT), 204);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidUpdates")
	void shouldRejectInvalidUpdatesWithoutChangingTheProject(InputCase input) throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);

		HttpResult result = exchange("PUT", PROJECT, input.body(), ContentType.APPLICATION_JSON);

		assertJson(result, 400, "{\"message\":\"" + input.message() + "\",\"error\":\""
				+ input.error() + "\"}");
		assertJson(exchange("GET", PROJECT), 200, fixture("project.json"));
	}

	private static Stream<InputCase> invalidUpdates()
	{
		return Stream.of(
				InputCase.builder().name("malformed JSON").body("{").build(),
				InputCase.builder().name("empty body").body("").build(),
				InputCase.builder().name("wrong scalar type").body("{\"public\":{}}").build(),
				InputCase.builder().name("unknown property").body("{\"unknown\":true}").build(),
				InputCase.builder().name("absent displayed name").body("{}")
						.message("Displayed name have to be set").error("IllegalArgumentException").build());
	}

	@Test
	void shouldPreserveNullBodyServerErrorWithoutChangingTheProject() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);

		assertEmpty(exchange("PUT", PROJECT, "null", ContentType.APPLICATION_JSON), 500);
		assertJson(exchange("GET", PROJECT), 200, fixture("project.json"));
	}

	@Test
	void shouldRejectAnUnacceptableResponseMediaType() throws Exception
	{
		HttpUriRequestBase request = new HttpUriRequestBase("GET", java.net.URI.create(PROJECTS));
		request.setHeader("Accept", "application/xml");

		try (var response = client.executeOpen(host, request, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(406);
		}
	}

	private record InputCase(String name, String body, String message, String error)
	{
		static Builder builder()
		{
			return new Builder();
		}

		@Override
		public String toString()
		{
			return name;
		}

		private static class Builder
		{
			private String name;
			private String body;
			private String message = "Can't perform JSON deserialization";
			private String error = "BadRequestException";

			Builder name(String name)
			{
				this.name = name;
				return this;
			}

			Builder body(String body)
			{
				this.body = body;
				return this;
			}

			Builder message(String message)
			{
				this.message = message;
				return this;
			}

			Builder error(String error)
			{
				this.error = error;
				return this;
			}

			InputCase build()
			{
				return new InputCase(name, body, message, error);
			}
		}
	}
}
