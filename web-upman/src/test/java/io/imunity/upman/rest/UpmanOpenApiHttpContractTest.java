package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;

class UpmanOpenApiHttpContractTest extends UpmanHttpContractTestBase
{
	@Test
	void shouldPublishTypedProjectTransportWithSpecificationMode() throws Exception
	{
		setPublication("specification");

		HttpResult result = anonymousGet("openapi.json");

		assertThat(anonymousGet("openapi.yaml").status()).isEqualTo(200);
		assertThat(anonymousGet("api-docs").status()).isEqualTo(404);
		assertThat(anonymousGet("api-docs/scalar.js").status()).isEqualTo(404);

		assertThat(result.status()).isEqualTo(200);
		JsonNode api = m.readTree(result.body());
		assertThat(api).isEqualTo(m.readTree(io.swagger.v3.core.util.Json.pretty(UpmanOpenApi.export())));
		JsonNode paths = api.path("paths");
		assertThat(paths.properties()).hasSize(25);
		assertThat(api.at("/info/title").asText()).isEqualTo("Unity Project Management API");
		assertThat(api.at("/servers/0/url").asText()).isEqualTo(".");
		JsonNode schemas = api.at("/components/schemas");
		assertThat(schemas.path("Project").path("properties").propertyStream().map(java.util.Map.Entry::getKey))
				.containsExactlyInAnyOrder("projectId", "public", "displayedName", "description", "logoUrl",
						"enableSubprojects", "readOnlyAttributes", "registrationForm", "signUpEnquiry",
						"membershipUpdateEnquiry", "urlId");
		assertThat(schemas.at("/ProjectId/properties/id/writeOnly").asBoolean()).isFalse();
		assertThat(schemas.at("/ProjectCreateRequest/properties/public/type").asText()).isEqualTo("boolean");
		assertThat(schemas.at("/ProjectUpdateRequest/properties/displayedName/additionalProperties/type").asText())
				.isEqualTo("string");
		assertThat(schemas.at("/RestPolicyDocument/properties").propertyStream().map(java.util.Map.Entry::getKey))
				.containsExactlyInAnyOrder("id", "name", "displayedName", "mandatory", "contentType", "revision", "content");
		assertThat(schemas.at("/Invitation/properties/expiration/type").asText()).isEqualTo("number");
		assertThat(schemas.at("/Invitation/properties/lastSentTime/type").asText()).isEqualTo("number");
		assertThat(schemas.at("/Request/properties/requestedTime/type").asText()).isEqualTo("number");
		assertThat(schemas.at("/InvitationRequest/properties/expiration/anyOf").size()).isEqualTo(2);
		JsonNode memberVariants = paths.path("/v1/projects/{project-id}/members")
				.at("/get/responses/200/content/application~1json/schema/anyOf");
		assertThat(memberVariants.size()).isEqualTo(2);
		assertThat(memberVariants.get(0).path("type").asText()).isEqualTo("array");
		assertThat(memberVariants.get(1).path("type").asText()).isEqualTo("array");
		JsonNode collection = paths.path("/v1/projects");
		assertThat(collection.at("/get/responses/200/content/application~1json/schema/type").asText())
				.isEqualTo("array");
		assertThat(collection.at("/post/responses/200/content/application~1json/schema/$ref").asText())
				.isEqualTo("#/components/schemas/ProjectId");
		JsonNode project = paths.path("/v1/projects/{project-id}");
		assertThat(project.at("/get/responses/200/content/application~1json/schema/$ref").asText())
				.isEqualTo("#/components/schemas/Project");
		assertThat(project.at("/put/responses").has("204")).isTrue();
		assertThat(project.at("/delete/responses").has("204")).isTrue();
	}

	@Test
	void shouldPublishScalarAndKeepApiCallsAuthenticated() throws Exception
	{
		setPublication("specificationAndUI");

		for (String resource : new String[] {"openapi.json", "openapi.yaml", "api-docs", "api-docs/",
				"api-docs/index.html", "api-docs/scalar.js"})
			assertThat(anonymousGet(resource).status()).as(resource).isEqualTo(200);
		assertThat(anonymousGet("api-docs").body()).contains("Scalar.createApiReference", "openapi.json");
		assertJson(anonymousGet("v1/projects"), 400, """
				{"message":"Authentication failed","error":"Exception"}
				""");
		assertJson(exchange("GET", "/restupm/v1/projects"), 200, "[]");

		setPublication("disabled");

		assertThat(anonymousGet("openapi.json").status()).isEqualTo(404);
		assertThat(anonymousGet("api-docs").status()).isEqualTo(404);
	}

	@Test
	void shouldKeepAuthenticatedApiAvailableWithPublicationDisabledByDefault() throws Exception
	{
		for (String resource : new String[] {"openapi.json", "openapi.yaml", "api-docs", "api-docs/",
				"api-docs/index.html", "api-docs/scalar.js", "api-docs/swagger-ui.css",
				"swagger-ui.css", "swagger-ui-bundle.js"})
			assertThat(anonymousGet(resource).status()).as(resource).isEqualTo(404);
		assertJson(exchange("POST", "/restupm/v1/projects", fixture("project-create.json"),
				ContentType.APPLICATION_JSON), 200, "{\"id\":\"contract\"}");
		assertJson(exchange("GET", "/restupm/v1/projects/contract"), 200, fixture("project.json"));
	}

	private void setPublication(String mode) throws Exception
	{
		endpointMan.updateEndpoint("restUpman", new EndpointConfiguration(null, null, null,
				CONFIGURATION + "unity.endpoint.rest.openApiPublication=" + mode + "\n", null));
	}

	private HttpResult anonymousGet(String resource) throws Exception
	{
		var request = new HttpGet("/restupm/" + resource);
		try (var response = client.executeOpen(host, request, HttpClientContext.create()))
		{
			var entity = response.getEntity();
			return new HttpResult(response.getCode(), entity == null ? null : entity.getContentType(),
					entity == null ? "" : EntityUtils.toString(entity));
		}
	}
}
