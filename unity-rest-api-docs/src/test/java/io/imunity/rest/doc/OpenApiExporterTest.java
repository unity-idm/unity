package io.imunity.rest.doc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.imunity.upman.rest.UpmanOpenApi;
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

		for (String file : new String[] {"upman-openapi-v1.yaml", "upman-openapi-v1.html"})
			assertThat(Files.readString(first.resolve(file))).isEqualTo(Files.readString(second.resolve(file)));
		assertThat(Files.readString(first.resolve("upman-openapi-v1.html")))
				.contains("Redoc.init", "disableGoogleFont: true", "Content-Type: application/json")
				.doesNotContain("https://cdn", "SwaggerUI", "</script><script");
		assertThat(Files.exists(first.resolve("admin-openapi-v1.yaml"))).isFalse();
		assertThat(Files.size(first.resolve("openapi-assets/redoc.standalone.js"))).isGreaterThan(100_000);
		assertThat(Files.readString(first.resolve("openapi-assets/redoc.standalone.js.LICENSE.txt")))
				.contains("license");
	}

	@Test
	void shouldRemoveObsoleteGeneratedSwaggerAssetsAndPreserveOtherAssets() throws Exception
	{
		Path assets = Files.createDirectories(temporary.resolve("openapi-assets"));
		Files.writeString(assets.resolve("swagger-ui-bundle.js"), "obsolete renderer");
		Files.writeString(assets.resolve("swagger-ui.css"), "obsolete stylesheet");
		Files.writeString(assets.resolve("custom.css"), "custom stylesheet");

		OpenApiExporter.generate(temporary);

		assertThat(assets.resolve("swagger-ui-bundle.js")).doesNotExist();
		assertThat(assets.resolve("swagger-ui.css")).doesNotExist();
		assertThat(Files.readString(assets.resolve("custom.css"))).isEqualTo("custom stylesheet");
		assertThat(Files.size(assets.resolve("redoc.standalone.js"))).isGreaterThan(100_000);
		assertThat(assets.resolve("redoc.standalone.js.LICENSE.txt")).isNotEmptyFile();
	}

	@Test
	void shouldRejectExamplesThatDisagreeWithThePublishedPropertyTypes()
	{
		var api = UpmanOpenApi.export();
		var document = Json.mapper().valueToTree(api);
		var schema = JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/Project");
		var input = JsonNodeFactory.instance.objectNode().put("projectId", 123);

		assertThatThrownBy(() -> OpenApiValidation.validateExample(document, schema, input, "project fixture"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("projectId");
	}

	@Test
	void shouldRejectAnUnknownLayoutDiscriminator()
	{
		var api = UpmanOpenApi.export();
		var media = api.getPaths().get("/v1/projects/{project-id}/registrationForm").getPost()
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
	void shouldRejectBodiesWithDescriptionsButNoStructuralSchema()
	{
		var api = UpmanOpenApi.export();
		api.getPaths().get("/v1/projects").getGet().getResponses().get("200")
				.getContent().get("application/json")
				.setSchema(new io.swagger.v3.oas.models.media.Schema<>().description("Unspecified object"));

		assertThatThrownBy(() -> OpenApiValidation.validate(api))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("Opaque body schema");
	}

	@Test
	void shouldRejectObjectBodiesWithNoProperties()
	{
		var api = UpmanOpenApi.export();
		api.getPaths().get("/v1/projects").getGet().getResponses().get("200")
				.getContent().get("application/json").setSchema(new io.swagger.v3.oas.models.media.ObjectSchema());

		assertThatThrownBy(() -> OpenApiValidation.validate(api))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("Empty object");
	}

	@Test
	void shouldRejectDuplicateOperationIds()
	{
		var api = UpmanOpenApi.export();
		api.getPaths().get("/v1/projects").getPost().setOperationId("getProjects");

		assertThatThrownBy(() -> OpenApiValidation.validate(api)).isInstanceOf(IllegalStateException.class);
	}
}
