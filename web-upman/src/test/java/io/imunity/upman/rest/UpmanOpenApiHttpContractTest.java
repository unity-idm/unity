package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

class UpmanOpenApiHttpContractTest extends UpmanHttpContractTestBase
{
	@Test
	void shouldDescribeTypedProjectTransportAtTheAuthenticatedEndpoint() throws Exception
	{
		HttpResult result = exchange("GET", "/restupm/openapi.json");

		assertThat(result.status()).isEqualTo(200);
		JsonNode api = m.readTree(result.body());
		JsonNode paths = api.path("paths");
		assertThat(paths.properties()).hasSize(25);
		assertThat(api.at("/info/title").asText()).isEqualTo("Unity Project Management API");
		assertThat(api.at("/servers/0/url").asText()).isEqualTo(".");
		JsonNode schemas = api.at("/components/schemas");
		assertThat(schemas.path("RestProject").path("properties").propertyStream().map(java.util.Map.Entry::getKey))
				.containsExactlyInAnyOrder("projectId", "public", "displayedName", "description", "logoUrl",
						"enableSubprojects", "readOnlyAttributes", "registrationForm", "signUpEnquiry",
						"membershipUpdateEnquiry", "urlId");
		assertThat(schemas.at("/RestProjectId/properties/id/writeOnly").asBoolean()).isFalse();
		assertThat(schemas.at("/RestProjectCreateRequest/properties/public/type").asText()).isEqualTo("boolean");
		assertThat(schemas.at("/RestProjectUpdateRequest/properties/displayedName/additionalProperties/type").asText())
				.isEqualTo("string");
		JsonNode collection = paths.path("/v1/projects");
		assertThat(collection.at("/get/responses/200/content/application~1json/schema/type").asText())
				.isEqualTo("array");
		assertThat(collection.at("/post/responses/200/content/application~1json/schema/$ref").asText())
				.isEqualTo("#/components/schemas/RestProjectId");
		JsonNode project = paths.path("/v1/projects/{project-id}");
		assertThat(project.at("/get/responses/200/content/application~1json/schema/$ref").asText())
				.isEqualTo("#/components/schemas/RestProject");
		assertThat(project.at("/put/responses").has("204")).isTrue();
		assertThat(project.at("/delete/responses").has("204")).isTrue();
	}
}
