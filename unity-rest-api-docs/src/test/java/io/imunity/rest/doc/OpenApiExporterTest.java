package io.imunity.rest.doc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.imunity.rest.admin.AdminOpenApi;
import io.swagger.v3.core.util.Json;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenApiExporterTest
{
	@TempDir
	Path temporary;

	@Test
	void shouldExportDeterministicValidatedYamlAndOfflineHtml() throws Exception
	{
		Path first = temporary.resolve("first");
		Path second = temporary.resolve("second");

		OpenApiExporter.generate(first);
		OpenApiExporter.generate(second);

		for (String file : new String[] {"admin-openapi-v1.yaml", "admin-openapi-v1.html"})
			assertThat(Files.readString(first.resolve(file))).isEqualTo(Files.readString(second.resolve(file)));
		assertThat(Files.readString(first.resolve("admin-openapi-v1.html")))
				.contains("SwaggerUIBundle", "Unity Administration API")
				.doesNotContain("https://cdn", "</script><script");
		assertThat(Files.exists(first.resolve("upman-openapi-v1.yaml"))).isFalse();
		assertThat(Files.size(first.resolve("openapi-assets/swagger-ui-bundle.js"))).isGreaterThan(100_000);
		assertThat(Files.size(first.resolve("openapi-assets/swagger-ui.css"))).isGreaterThan(10_000);
	}

	@Test
	void shouldRejectExamplesThatDisagreeWithThePublishedPropertyTypes()
	{
		var api = AdminOpenApi.export();
		var document = Json.mapper().valueToTree(api);
		var schema = JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/RestEntityInformation");
		var input = JsonNodeFactory.instance.objectNode().put("entityId", "not-a-number");

		assertThatThrownBy(() -> OpenApiValidation.validateExample(document, schema, input, "entity fixture"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("entityId");
	}

	@Test
	void shouldRejectAnUnknownLayoutDiscriminator()
	{
		var api = AdminOpenApi.export();
		var media = api.getPaths().get("/v1/registrationForm").getPost()
				.getRequestBody().getContent().get("application/json");
		var example = (com.fasterxml.jackson.databind.node.ObjectNode) Json.mapper().valueToTree(media.getExample());
		((com.fasterxml.jackson.databind.node.ObjectNode) example.at("/RegistrationFormLayouts/primaryLayout/elements/0"))
				.put("clazz", "unknown-layout");
		var document = Json.mapper().valueToTree(api);
		var schema = Json.mapper().valueToTree(media.getSchema());

		assertThatThrownBy(() -> OpenApiValidation.validateExample(document, schema, example, "layout fixture"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldRejectStringTimestampsInFlattenedAdminInvitations()
	{
		var api = AdminOpenApi.export();
		var media = api.getPaths().get("/v1/invitation/{code}").getGet().getResponses().get("200")
				.getContent().get("application/json");
		var example = (com.fasterxml.jackson.databind.node.ObjectNode) Json.mapper().valueToTree(media.getExample());
		example.put("lastSentTime", "2026-01-01T00:00:00Z");

		assertThatThrownBy(() -> OpenApiValidation.validateExample(Json.mapper().valueToTree(api),
				Json.mapper().valueToTree(media.getSchema()), example, "admin invitation fixture"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldRejectAdminPoliciesWithoutRequiredFields()
	{
		var api = AdminOpenApi.export();
		var media = api.getPaths().get("/v1/policy-documents").getPost().getRequestBody()
				.getContent().values().iterator().next();
		var example = (com.fasterxml.jackson.databind.node.ObjectNode) Json.mapper().valueToTree(media.getExample());
		example.remove("mandatory");

		assertThatThrownBy(() -> OpenApiValidation.validateExample(Json.mapper().valueToTree(api),
				Json.mapper().valueToTree(media.getSchema()), example, "admin policy fixture"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("mandatory");
	}

	@Test
	void shouldRejectBodiesWithDescriptionsButNoStructuralSchema()
	{
		var api = AdminOpenApi.export();
		api.getPaths().get("/v1/policy-documents").getGet().getResponses().get("200")
				.getContent().get("application/json")
				.setSchema(new io.swagger.v3.oas.models.media.Schema<>().description("Unspecified object"));

		assertThatThrownBy(() -> OpenApiValidation.validate(api))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("Opaque body schema");
	}

	@Test
	void shouldRejectObjectBodiesWithNoProperties()
	{
		var api = AdminOpenApi.export();
		api.getPaths().get("/v1/policy-documents").getGet().getResponses().get("200")
				.getContent().get("application/json").setSchema(new io.swagger.v3.oas.models.media.ObjectSchema());

		assertThatThrownBy(() -> OpenApiValidation.validate(api))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("Empty object");
	}

	@Test
	void shouldRejectDuplicateOperationIds()
	{
		var api = AdminOpenApi.export();
		api.getPaths().get("/v1/policy-documents").getPost().setOperationId("getPolicyDocuments");

		assertThatThrownBy(() -> OpenApiValidation.validate(api)).isInstanceOf(IllegalStateException.class);
	}
}
