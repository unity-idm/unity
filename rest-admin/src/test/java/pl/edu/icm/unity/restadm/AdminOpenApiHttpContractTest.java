package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;

import io.imunity.rest.admin.AdminOpenApi;
import io.swagger.v3.core.util.Json;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;

class AdminOpenApiHttpContractTest extends AdminHttpContractTestBase
{
	@Test
	void shouldExportTheSameIsolatedAdminSchemaAsTheLiveApplication() throws Exception
	{
		setPublication("specification");

		HttpResult result = anonymousGet("openapi.json");

		assertThat(anonymousGet("openapi.yaml").status()).isEqualTo(200);
		assertThat(anonymousGet("api-docs").status()).isEqualTo(404);

		assertThat(result.status()).as(result.body()).isEqualTo(200);
		var document = m.readTree(result.body());
		assertThat(document).isEqualTo(m.readTree(Json.pretty(AdminOpenApi.export())));
		assertThat(document.at("/info/title").asText()).isEqualTo("Unity Administration API");
		assertThat(document.at("/servers/0/url").asText()).isEqualTo(".");
		assertThat(document.path("paths").propertyStream().map(java.util.Map.Entry::getKey))
				.noneMatch(path -> path.contains("/projects") || path.contains("openapi"));
		assertThat(document.path("paths").path("/v1/policy-documents")
				.at("/post/responses/200/content/application~1json/schema/$ref").asText())
				.isEqualTo("#/components/schemas/RestPolicyDocumentId");
	}

	@Test
	void shouldServeScalarPubliclyAndKeepAdminApiAuthenticated() throws Exception
	{
		setPublication("specificationAndUI");

		HttpResult result = anonymousGet("api-docs");

		assertThat(result.status()).as(result.body()).isEqualTo(200);
		assertThat(result.body()).contains("Scalar.createApiReference", "api-docs/scalar.js", "openapi.json",
				"<base href=\"/restadm/\">");
		assertThat(anonymousGet("api-docs/scalar.js").status()).isEqualTo(200);
		assertThat(anonymousGet("openapi.json").status()).isEqualTo(200);
		assertJson(anonymousGet("v1/policy-documents"), 400, """
				{"message":"Authentication failed","error":"Exception"}
				""");
		assertThat(exchange("GET", "/restadm/v1/policy-documents").status()).isEqualTo(200);
	}

	@Test
	void shouldDisablePublicationByDefault() throws Exception
	{
		for (String resource : new String[] {"openapi.json", "openapi.yaml", "api-docs", "api-docs/",
				"api-docs/index.html", "api-docs/scalar.js"})
			assertThat(anonymousGet(resource).status()).as(resource).isEqualTo(404);
	}

	private void setPublication(String mode) throws Exception
	{
		endpointMan.updateEndpoint("restAdmin", new EndpointConfiguration(null, null, null,
				"unity.endpoint.rest.openApiPublication=" + mode + "\n", null));
	}

	private HttpResult anonymousGet(String resource) throws Exception
	{
		var request = new HttpGet("/restadm/" + resource);
		try (var response = client.executeOpen(host, request, HttpClientContext.create()))
		{
			var entity = response.getEntity();
			return new HttpResult(response.getCode(), entity == null ? null : entity.getContentType(),
					entity == null ? "" : EntityUtils.toString(entity));
		}
	}
}
