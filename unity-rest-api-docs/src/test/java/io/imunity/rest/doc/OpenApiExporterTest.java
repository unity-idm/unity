package io.imunity.rest.doc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.admin.AdminOpenApi;
import io.imunity.upman.rest.UpmanOpenApi;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;

class OpenApiExporterTest
{
	@TempDir
	Path temporary;

	@Test
	void shouldExportBothApisAsDeterministicValidatedYamlAndOfflineHtml() throws Exception
	{
		Path first = temporary.resolve("first");
		Path second = temporary.resolve("second");

		OpenApiExporter.generate(first);
		OpenApiExporter.generate(second);

		for (String api : List.of("admin", "upman"))
		{
			for (String extension : List.of("yaml", "html"))
			{
				String file = api + "-openapi-v1." + extension;
				assertThat(Files.readString(first.resolve(file))).isEqualTo(Files.readString(second.resolve(file)));
			}
			assertThat(Files.readString(first.resolve(api + "-openapi-v1.html")))
					.contains("Redoc.init", "disableGoogleFont: true")
					.doesNotContain("https://cdn", "SwaggerUI", "</script><script");
		}
		assertThat(Files.readString(first.resolve("admin-openapi-v1.html"))).contains("Unity Administration API");
		assertThat(Files.readString(first.resolve("upman-openapi-v1.html")))
				.contains("Unity Project Management API", "Content-Type: application/json");
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
	void shouldRejectProjectExamplesThatDisagreeWithThePublishedPropertyTypes()
	{
		var document = Json.mapper().valueToTree(UpmanOpenApi.export());
		var schema = JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/Project");
		var input = JsonNodeFactory.instance.objectNode().put("projectId", 123);

		assertThatThrownBy(() -> OpenApiValidation.validateExample(document, schema, input, "project fixture"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("projectId");
	}

	@Test
	void shouldRejectEntityExamplesThatDisagreeWithThePublishedPropertyTypes()
	{
		var document = Json.mapper().valueToTree(AdminOpenApi.export());
		var schema = JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/RestEntityInformation");
		var input = JsonNodeFactory.instance.objectNode().put("entityId", "not-a-number");

		assertThatThrownBy(() -> OpenApiValidation.validateExample(document, schema, input, "entity fixture"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("entityId");
	}

	@ParameterizedTest(name = "layout discriminator: {0}")
	@MethodSource("apis")
	void shouldRejectAnUnknownLayoutDiscriminator(ApiCase scenario)
	{
		var api = scenario.exporter().get();
		var media = api.getPaths().get(scenario.formPath()).getPost()
				.getRequestBody().getContent().get("application/json");
		var example = (ObjectNode) Json.mapper().valueToTree(media.getExample());
		((ObjectNode) example.at("/RegistrationFormLayouts/primaryLayout/elements/0"))
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
		var example = (ObjectNode) Json.mapper().valueToTree(media.getExample());
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
		var example = (ObjectNode) Json.mapper().valueToTree(media.getExample());
		example.remove("mandatory");

		assertThatThrownBy(() -> OpenApiValidation.validateExample(Json.mapper().valueToTree(api),
				Json.mapper().valueToTree(media.getSchema()), example, "admin policy fixture"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("mandatory");
	}

	@ParameterizedTest(name = "opaque body: {0}")
	@MethodSource("apis")
	void shouldRejectBodiesWithDescriptionsButNoStructuralSchema(ApiCase scenario)
	{
		var api = scenario.exporter().get();
		api.getPaths().get(scenario.collectionPath()).getGet().getResponses().get("200")
				.getContent().get("application/json")
				.setSchema(new io.swagger.v3.oas.models.media.Schema<>().description("Unspecified object"));

		assertThatThrownBy(() -> OpenApiValidation.validate(api))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("Opaque body schema");
	}

	@ParameterizedTest(name = "empty object: {0}")
	@MethodSource("apis")
	void shouldRejectObjectBodiesWithNoProperties(ApiCase scenario)
	{
		var api = scenario.exporter().get();
		api.getPaths().get(scenario.collectionPath()).getGet().getResponses().get("200")
				.getContent().get("application/json").setSchema(new io.swagger.v3.oas.models.media.ObjectSchema());

		assertThatThrownBy(() -> OpenApiValidation.validate(api))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("Empty object");
	}

	@ParameterizedTest(name = "duplicate operation: {0}")
	@MethodSource("apis")
	void shouldRejectDuplicateOperationIds(ApiCase scenario)
	{
		var api = scenario.exporter().get();
		var collection = api.getPaths().get(scenario.collectionPath());
		collection.getPost().setOperationId(collection.getGet().getOperationId());

		assertThatThrownBy(() -> OpenApiValidation.validate(api)).isInstanceOf(IllegalStateException.class);
	}

	private static Stream<ApiCase> apis()
	{
		return Stream.of(
				ApiCase.builder().name("Admin").exporter(AdminOpenApi::export)
						.collectionPath("/v1/policy-documents").formPath("/v1/registrationForm").build(),
				ApiCase.builder().name("UpMan").exporter(UpmanOpenApi::export)
						.collectionPath("/v1/projects").formPath("/v1/projects/{project-id}/registrationForm").build());
	}

	private record ApiCase(String name, Supplier<OpenAPI> exporter, String collectionPath, String formPath)
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
			private Supplier<OpenAPI> exporter;
			private String collectionPath;
			private String formPath;

			Builder name(String name)
			{
				this.name = name;
				return this;
			}

			Builder exporter(Supplier<OpenAPI> exporter)
			{
				this.exporter = exporter;
				return this;
			}

			Builder collectionPath(String collectionPath)
			{
				this.collectionPath = collectionPath;
				return this;
			}

			Builder formPath(String formPath)
			{
				this.formPath = formPath;
				return this;
			}

			ApiCase build()
			{
				return new ApiCase(name, exporter, collectionPath, formPath);
			}
		}
	}
}
