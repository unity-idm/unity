package io.imunity.rest.doc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.OpenApi30;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class OpenApiValidation
{
	static void validate(OpenAPI api)
	{
		var parsed = new OpenAPIV3Parser().readContents(Json.pretty(api), null, new ParseOptions());
		require(parsed.getOpenAPI() != null && parsed.getMessages().isEmpty(), parsed.getMessages().toString());
		JsonNode document = Json.mapper().valueToTree(api);
		validateReferences(document, document);
		Set<String> ids = new HashSet<>();
		List<String> failures = new ArrayList<>();
		api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
			String location = method + " " + path;
			require(operation.getOperationId() != null && !operation.getOperationId().isBlank()
					&& ids.add(operation.getOperationId()),
					"Missing or duplicate operationId: " + location);
			require(operation.getSummary() != null && operation.getDescription() != null,
					"Missing operation explanation: " + location);
			require(!operation.getResponses().containsKey("default"), "Unspecified response: " + location);
			require(operation.getResponses().keySet().stream().anyMatch(status -> status.startsWith("2")),
					"Missing success response: " + location);
			require(operation.getResponses().keySet().containsAll(Set.of("400", "401", "403", "500")),
					"Missing explicit error responses: " + location);
			if (operation.getRequestBody() != null)
				operation.getRequestBody().getContent().forEach((mediaType, media) -> {
					JsonNode schema = Json.mapper().valueToTree(media.getSchema());
					validateBody(document, schema, location + " request", false);
					if (media.getExample() != null)
						collectExampleErrors(document, schema, media.getExample(), location, failures);
				});
			operation.getResponses().forEach((status, response) -> {
				if (status.startsWith("2") && response.getContent() != null)
					response.getContent().forEach((mediaType, media) -> {
						JsonNode schema = Json.mapper().valueToTree(media.getSchema());
						validateBody(document, schema, location + " response", !mediaType.equals("application/json"));
						if (media.getExample() != null)
							collectExampleErrors(document, schema, media.getExample(), location, failures);
						if (media.getExamples() != null)
							media.getExamples().forEach((name, example) -> collectExampleErrors(document, schema,
									example.getValue(), location + " " + name, failures));
					});
			});
		}));
		require(failures.isEmpty(), String.join("\n", failures));
	}

	private static void collectExampleErrors(JsonNode document, JsonNode schema, Object example, String location,
			List<String> failures)
	{
		try
		{
			validateExample(document, schema, Json.mapper().valueToTree(example), location);
		} catch (IllegalStateException e)
		{
			failures.add(e.getMessage());
		}
	}

	static void validateExample(JsonNode document, JsonNode schema, JsonNode example, String location)
	{
		ObjectNode root = schema.deepCopy();
		root.set("components", document.get("components"));
		var errors = SchemaRegistry.withDefaultDialect(OpenApi30.getInstance()).getSchema(root).validate(example);
		require(errors.isEmpty(), location + ": " + errors);
	}

	private static void validateBody(JsonNode document, JsonNode schema, String location, boolean allowScalar)
	{
		if (schema.has("$ref"))
		{
			validateBody(document, document.at(schema.path("$ref").asText().substring(1)), location, allowScalar);
			return;
		}
		require((schema.has("type") || schema.has("anyOf") || schema.has("oneOf") || schema.has("allOf"))
				&& (!schema.path("type").asText().equals("string") || allowScalar
				|| schema.path("format").asText().equals("binary")),
				"Opaque body schema: " + location + " " + schema);
		if (schema.path("type").asText().equals("object"))
			require(!schema.path("properties").isEmpty() || schema.path("additionalProperties").asBoolean()
					|| !schema.path("additionalProperties").isEmpty(), "Empty object: " + location);
		if (schema.path("type").asText().equals("array"))
			validateBody(document, schema.path("items"), location, true);
		for (String composition : new String[] {"anyOf", "oneOf", "allOf"})
			if (schema.has(composition))
				schema.get(composition).forEach(branch -> validateBody(document, branch, location, allowScalar));
	}

	private static void validateReferences(JsonNode document, JsonNode node)
	{
		if (node.isObject() && node.has("$ref"))
		{
			String reference = node.get("$ref").asText();
			require(reference.startsWith("#/") && !document.at(reference.substring(1)).isMissingNode(),
					"Unresolved or external reference: " + reference);
		}
		node.elements().forEachRemaining(child -> validateReferences(document, child));
	}

	private static void require(boolean condition, String message)
	{
		if (!condition)
			throw new IllegalStateException(message);
	}
}
