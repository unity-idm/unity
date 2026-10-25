package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.imunity.rest.admin.AdminOpenApi;
import io.swagger.v3.core.util.Json;

class AdminOpenApiHttpContractTest extends AdminHttpContractTestBase
{
	@Test
	void shouldExportTheSameIsolatedAdminSchemaAsTheLiveApplication() throws Exception
	{
		HttpResult result = exchange("GET", "/restadm/openapi.json");

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
}
