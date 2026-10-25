package io.imunity.rest.doc;

import com.fasterxml.jackson.databind.SerializationFeature;

import io.imunity.upman.rest.UpmanOpenApi;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;

public final class OpenApiExporter
{
	public static void main(String... arguments) throws IOException
	{
		if (arguments.length != 1)
			throw new IllegalArgumentException("Expected the output directory");
		generate(Path.of(arguments[0]));
	}

	static void generate(Path output) throws IOException
	{
		Files.createDirectories(output);
		copyRenderer(output);
		generate(UpmanOpenApi.export(), "upman-openapi-v1", output);
	}

	private static void generate(OpenAPI api, String name, Path output) throws IOException
	{
		OpenApiValidation.validate(api);
		Files.createDirectories(output);
		String yaml = Yaml.mapper().writer().with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
				.writeValueAsString(api);
		String json = Json.mapper().writer().with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
				.writeValueAsString(api);
		Files.writeString(output.resolve(name + ".yaml"), yaml, StandardCharsets.UTF_8);
		String safeJson = json.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
		Files.writeString(output.resolve(name + ".html"), """
				<!doctype html>
				<html lang="en"><head><meta charset="utf-8">
				<meta name="viewport" content="width=device-width, initial-scale=1">
				<title>%s</title>
				<link rel="stylesheet" href="openapi-assets/swagger-ui.css"></head>
				<body><div id="swagger-ui"></div>
				<script src="openapi-assets/swagger-ui-bundle.js"></script>
				<script>SwaggerUIBundle({dom_id:'#swagger-ui',deepLinking:true,validatorUrl:null,supportedSubmitMethods:[],spec:
				""".formatted(api.getInfo().getTitle()) + safeJson + "});</script></body></html>\n", StandardCharsets.UTF_8);
	}

	private static void copyRenderer(Path output) throws IOException
	{
		Properties artifact = new Properties();
		try (var input = Objects.requireNonNull(OpenApiExporter.class.getResourceAsStream(
				"/META-INF/maven/org.webjars/swagger-ui/pom.properties")))
		{
			artifact.load(input);
		}
		Path assets = Files.createDirectories(output.resolve("openapi-assets"));
		for (String file : new String[] {"swagger-ui.css", "swagger-ui-bundle.js"})
			try (var input = Objects.requireNonNull(OpenApiExporter.class.getResourceAsStream(
					"/META-INF/resources/webjars/swagger-ui/" + artifact.getProperty("version") + "/" + file)))
			{
				Files.copy(input, assets.resolve(file), StandardCopyOption.REPLACE_EXISTING);
			}
	}
}
