package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

class UpmanOpenApiHttpContractTest extends UpmanHttpContractTestBase
{
	@Test
	void shouldDescribeTypedProjectTransportAtTheAuthenticatedEndpoint() throws Exception
	{
		HttpResult result = exchange("GET", "/restupm/openapi.json");

		assertThat(result.status()).isEqualTo(200);
		JsonNode api = m.readTree(result.body());
		assertThat(api).isEqualTo(m.readTree(io.swagger.v3.core.util.Json.pretty(UpmanOpenApi.export())));
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
		assertThat(schemas.at("/RestPolicyDocument/properties").propertyStream().map(java.util.Map.Entry::getKey))
				.containsExactlyInAnyOrder("id", "name", "displayedName", "mandatory", "contentType", "revision", "content");
		assertThat(schemas.at("/RestInvitation/properties/expiration/type").asText()).isEqualTo("number");
		assertThat(schemas.at("/RestInvitation/properties/lastSentTime/type").asText()).isEqualTo("number");
		assertThat(schemas.at("/RestRequest/properties/requestedTime/type").asText()).isEqualTo("number");
		assertThat(schemas.at("/RestInvitationRequest/properties/expiration/anyOf").size()).isEqualTo(2);
		JsonNode memberVariants = paths.path("/v1/projects/{project-id}/members")
				.at("/get/responses/200/content/application~1json/schema/anyOf");
		assertThat(memberVariants.size()).isEqualTo(2);
		assertThat(memberVariants.get(0).path("type").asText()).isEqualTo("array");
		assertThat(memberVariants.get(1).path("type").asText()).isEqualTo("array");
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
	@Test
	void shouldRequireAuthenticationForLiveDocumentation() throws Exception
	{
		for (String format : new String[] {"json", "yaml"})
		{
			var request = new org.apache.hc.client5.http.classic.methods.HttpGet("/restupm/openapi." + format);
			try (var response = client.executeOpen(host, request,
					org.apache.hc.client5.http.protocol.HttpClientContext.create()))
			{
				assertThat(response.getCode()).isEqualTo(400);
				assertThat(m.readTree(org.apache.hc.core5.http.io.entity.EntityUtils.toString(response.getEntity())))
						.isEqualTo(m.readTree("""
								{"message":"Authentication failed","error":"Exception"}
								"""));
			}
		}
	}

}
