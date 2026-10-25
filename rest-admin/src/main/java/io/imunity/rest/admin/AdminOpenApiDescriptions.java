package io.imunity.rest.admin;

import io.imunity.rest.openapi.FormOpenApiSchemas;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

final class AdminOpenApiDescriptions
{
	private static final Properties OPERATIONS = load();
	private static final Map<String, String> PARAMETERS = Map.ofEntries(
			Map.entry("entityId", "Entity ID, persistent ID, or identity value when identityType is supplied."),
			Map.entry("identityType", "Identity type for identity-based addressing; omitted for entity or persistent IDs."),
			Map.entry("identityValue", "Value of the identity to address."),
			Map.entry("identity", "Identity value to import."),
			Map.entry("type", "Identity or token type, as selected by this operation."),
			Map.entry("value", "Identity or token value; URL-encode reserved characters."),
			Map.entry("credentialRequirement", "Credential requirement assigned to the new entity."),
			Map.entry("credential", "Name of the configured credential."),
			Map.entry("when", "Scheduled time in milliseconds since the Unix epoch."),
			Map.entry("operation", "Case-sensitive scheduled operation name."),
			Map.entry("status", "Desired state; interpreted according to the operation's resource."),
			Map.entry("target", "Target of a targeted identity."),
			Map.entry("realm", "Realm of a targeted identity."),
			Map.entry("group", "Group path; defaults to the root where supported by the operation."),
			Map.entry("groupPath", "Group path. URL-encode embedded path separators."),
			Map.entry("rootGroupPath", "Root group path beneath which bulk membership is queried."),
			Map.entry("groups", "Repeated query parameter selecting group paths."),
			Map.entry("attributes", "Repeated query parameter selecting attribute names."),
			Map.entry("attributeName", "Name of the selected attribute."),
			Map.entry("groupsPatterns", "Repeated group-pattern selector; /** matches all groups."),
			Map.entry("effective", "Include effective attributes; defaults to true."),
			Map.entry("includeSimpleValues", "Include externally converted simpleValues; defaults to false."),
			Map.entry("recursive", "Also remove descendant groups; defaults to false."),
			Map.entry("withParents", "Create missing ancestor groups; defaults to false."),
			Map.entry("toRemove", "Name of the attribute type to remove."),
			Map.entry("withInstances", "Also remove assigned instances; defaults to false."),
			Map.entry("templateId", "Identifier of the configured message template."),
			Map.entry("id", "Endpoint name."),
			Map.entry("typeId", "Identifier of the endpoint type to deploy."),
			Map.entry("address", "Context address at which to deploy the endpoint."),
			Map.entry("formId", "Stored form name."),
			Map.entry("dropRequests", "Also remove requests attached to the form; defaults to false."),
			Map.entry("ignoreRequestsAndInvitations",
					"Allow updating despite existing requests and invitations; false by default."),
			Map.entry("ignoreInvitations", "Historical accepted parameter; currently unused."),
			Map.entry("requestId", "Stored registration request ID."),
			Map.entry("responseId", "Stored enquiry response ID."),
			Map.entry("code", "Stored invitation code."),
			Map.entry("timeout", "Wait timeout in milliseconds; omitted or negative selects asynchronous execution."),
			Map.entry("importer", "Importer name; omitted to try all applicable importers."),
			Map.entry("eventName", "Name of the event to publish."),
			Map.entry("owner", "Token owner's entity ID or identity value."),
			Map.entry("ownerType", "Identity type used to interpret owner."),
			Map.entry("since", "Start time in milliseconds since the Unix epoch."),
			Map.entry("groupBy", "Statistic grouping; defaults to none."),
			Map.entry("skipZeroRecords", "Exclude zero-count statistics; defaults to true."),
			Map.entry("document-id", "Numeric policy document ID, passed as a decimal string."),
			Map.entry("incrementRevision", "Required flag: true increments the revision; false preserves it."),
			Map.entry("Content-Length", "Optional byte count; the size limit is enforced against actual bytes too."),
			Map.entry("systemConfig", "Include system configuration in the backup; defaults to true."),
			Map.entry("directorySchema", "Include directory schema in the backup; defaults to true."),
			Map.entry("users", "Include user data in the backup; defaults to true."),
			Map.entry("auditLogs", "Include audit logs in the backup; defaults to true."),
			Map.entry("signupRequests", "Include signup requests in the backup; defaults to true."),
			Map.entry("idpStatistics", "Include identity-provider statistics in the backup; defaults to true."));

	static void apply(OpenAPI api)
	{
		FormOpenApiSchemas.apply(api);
		api.getComponents().addSchemas("AdminError", new ObjectSchema()
				.addProperty("message", new StringSchema()).addProperty("error", new StringSchema())
				.required(List.of("message", "error")));
		api.getPaths().forEach((route, path) -> path.readOperations().forEach(operation -> {
			String id = operation.getOperationId();
			operation.setSummary(required(id + ".summary"));
			operation.setDescription(required(id + ".description"));
			operation.setTags(List.of(tag(route)));
			if (operation.getParameters() != null)
				operation.getParameters().forEach(parameter -> parameter.setDescription(
						Objects.requireNonNull(PARAMETERS.get(parameter.getName()), parameter.getName())));
			for (String status : List.of("400", "401", "403", "404", "406", "500"))
			{
				ApiResponse response = new ApiResponse().description(switch (status)
				{
					case "400" -> "Invalid argument or rejected operation, including missing direct authentication. "
							+ "Some JAX-RS binding errors have no body.";
					case "401" -> "Authentication is required by the deployed endpoint.";
					case "403" -> "The caller lacks administrative permission for this operation.";
					case "404" -> "The resource or route was not found; the body may be empty.";
					case "406" -> "The requested response media type is not supported.";
					default -> "Server error. Legacy InternalException and null-input failures can have empty bodies.";
				});
				if (Set.of("400", "403").contains(status))
					response.content(new Content().addMediaType("application/json", new MediaType()
							.schema(new Schema<>().$ref("#/components/schemas/AdminError"))));
				operation.getResponses().addApiResponse(status, response);
			}
			if (operation.getRequestBody() != null)
			{
				operation.getRequestBody().setRequired(true);
				if (operation.getRequestBody().getContent().keySet().equals(Set.of("application/json")))
					operation.getResponses().addApiResponse("415", new ApiResponse()
							.description("Content-Type must be application/json."));
			}
		}));
		api.getPaths().get("/v1/db-dump").getPost().getResponses().addApiResponse("413",
				new ApiResponse().description("The declared or actual backup size exceeds the configured limit."));
		api.getComponents().getSchemas().get("RestPolicyDocumentRequest")
				.setRequired(List.of("name", "displayedName", "mandatory", "contentType", "content"));
		api.getComponents().getSchemas().get("RestGroup").setRequired(List.of("path"));
	}

	private static String tag(String route)
	{
		if (route.contains("policy-documents"))
			return "Policy documents";
		if (route.contains("db-dump"))
			return "Backups";
		if (route.contains("invitation"))
			return "Invitations";
		if (route.contains("Form") || route.contains("Request") || route.contains("Response"))
			return "Forms and requests";
		if (route.contains("endpoint"))
			return "Endpoints";
		if (route.contains("token"))
			return "Tokens";
		if (route.contains("credential") || route.contains("2f-optin"))
			return "Credentials";
		if (route.contains("group") || route.contains("Group"))
			return "Groups and memberships";
		if (route.contains("attribute") || route.contains("Attribute"))
			return "Attributes";
		return "Entities and administration";
	}

	private static String required(String key)
	{
		return Objects.requireNonNull(OPERATIONS.getProperty(key), "Missing admin OpenAPI description: " + key);
	}

	private static Properties load()
	{
		Properties result = new Properties();
		try (var input = Objects.requireNonNull(AdminOpenApiDescriptions.class
				.getResourceAsStream("openapi/operations.properties"));
				var reader = new InputStreamReader(input, StandardCharsets.UTF_8))
		{
			result.load(reader);
			return result;
		} catch (IOException e)
		{
			throw new IllegalStateException("Cannot load admin OpenAPI descriptions", e);
		}
	}
}
