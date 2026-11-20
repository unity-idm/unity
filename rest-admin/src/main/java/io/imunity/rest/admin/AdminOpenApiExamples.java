package io.imunity.rest.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.IOException;
import java.util.Objects;

final class AdminOpenApiExamples
{
	static void apply(OpenAPI api)
	{
		api.getPaths().get("/v1/endpoint/{id}").getPost().getResponses().get("200")
				.getContent().get("application/json").setExample(read("endpoint.json"));
		api.getPaths().get("/v1/endpoints").getGet().getResponses().get("200")
				.getContent().get("application/json").setExample(Json.mapper().createArrayNode().add(read("endpoint.json")));
		api.getPaths().get("/v1/entity/{entityId}").getGet().getResponses().get("200")
				.getContent().get("application/json").setExample(read("entity.json"));
		api.getPaths().get("/v1/group/{groupPath}/meta").getGet().getResponses().get("200")
				.getContent().get("application/json").setExample(read("group.json"));
		api.getPaths().get("/v1/entity/{entityId}/attributes").getGet().getResponses().get("200")
				.getContent().get("application/json").setExample(read("attributes.json"));
		api.getPaths().get("/v1/invitation/{code}").getGet().getResponses().get("200")
				.getContent().get("application/json").setExample(read("invitation.json"));
		api.getPaths().get("/v1/registrationRequest/{requestId}").getGet().getResponses().get("200")
				.getContent().get("application/json").setExample(read("registration-request.json"));
		api.getPaths().get("/v1/tokens").getGet().getResponses().get("200")
				.getContent().get("application/json").setExample(Json.mapper().createArrayNode().add(read("token.json")));
		api.getPaths().get("/v1/invitation").getPost().getRequestBody().getContent()
				.get("application/json").setExample(read("invitation-create.json"));
		api.getPaths().get("/v1/policy-documents").getPost().getRequestBody().getContent().values()
				.forEach(media -> media.setExample(read("policy-create.json")));
		for (String kind : new String[] {"registration", "enquiry"})
		{
			String path = "/v1/" + kind + "Form";
			JsonNode example = read(kind + "-form-create.json");
			api.getPaths().get(path).getPost().getRequestBody().getContent().get("application/json")
					.setExample(example);
			ObjectNode response = example.deepCopy();
			if (kind.equals("registration"))
			{
				ObjectNode information = response.putObject("FormInformation2ndStage");
				information.putNull("DefaultValue");
				information.putObject("Map");
			}
			api.getPaths().get(path + "s").getGet().getResponses().get("200").getContent().get("application/json")
					.setExample(Json.mapper().createArrayNode().add(response));
		}
	}

	private static JsonNode read(String name)
	{
		try (var input = Objects.requireNonNull(AdminOpenApiExamples.class.getResourceAsStream("contract/" + name)))
		{
			return Json.mapper().readTree(input);
		} catch (IOException e)
		{
			throw new IllegalStateException("Cannot read admin example: " + name, e);
		}
	}
}
