package io.imunity.upman.rest;

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

final class UpmanOpenApiDescriptions
{
	private static final Properties OPERATIONS = loadOperations();
	private static final Map<String, String> PARAMETERS = Map.ofEntries(
			Map.entry("project-id", "Project identifier from the returned urlId; use it unchanged, "
					+ "including for nested or percent-sign names."),
			Map.entry("policy-id", "Numeric identifier of a policy attached to this project."),
			Map.entry("userId", "Member contact email address."),
			Map.entry("entityId", "Stable Unity entity identifier; supports users without an email address."),
			Map.entry("code", "Invitation code belonging to this project."),
			Map.entry("requestId", "Pending request identifier belonging to this project."),
			Map.entry("path", "Group path relative to the project. An absent or empty value selects the project root."),
			Map.entry("groupPath", "Relative group path; absent or empty selects the project root. On the member listing, "
					+ "presence also selects the entity-ID representation, even when the value is empty."),
			Map.entry("autogenerate", "Generate a form and ignore the body. Defaults to false. "
					+ "Content-Type: application/json is required even when the body is omitted."),
			Map.entry("ignoreRequests", "Allow updating a form with pending requests. Defaults to false. "
					+ "Preserved requests may become invalid after a form update."),
			Map.entry("dropRequests", "Remove pending requests together with the form. Defaults to false."),
			Map.entry("incrementRevision", "Increment the policy revision only when true. Defaults to false."),
			Map.entry("decision", "Accept or decline the stored pending request; no body is required."));

	static void apply(OpenAPI api)
	{
		addErrors(api);
		io.imunity.rest.openapi.FormOpenApiSchemas.apply(api);
		api.getPaths().forEach((route, path) -> path.readOperations().forEach(operation -> {
			String id = operation.getOperationId();
			operation.setTags(List.of(tagFor(route)));
			operation.setSummary(required(id + ".summary"));
			operation.setDescription(required(id + ".description"));
			if (operation.getParameters() != null)
				operation.getParameters().forEach(parameter -> {
					parameter.setDescription(Objects.requireNonNull(PARAMETERS.get(parameter.getName()),
							parameter.getName()));
					if (parameter.getName().equals("decision"))
					{
						parameter.setRequired(true);
						parameter.setSchema(new StringSchema()._enum(List.of("accept", "decline")));
					}
				});
			for (String status : List.of("400", "401", "403", "404", "406", "500"))
				operation.getResponses().addApiResponse(status,
						new ApiResponse().$ref("#/components/responses/Upman" + status));
			if (Set.of("resendInvitation", "setGroupMemberRole", "addProjectMemberAuthorizationRole",
					"addGroup", "updateGroup", "setGroupDelegation", "removeGroupMember").contains(id))
				operation.getResponses().addApiResponse("409", new ApiResponse().description(
						"The operation conflicts with the current state. The response may have an empty body.")
						.content(new Content().addMediaType("application/json", new MediaType()
								.schema(new Schema<>().$ref("#/components/schemas/UpmanError")))));
			if (operation.getRequestBody() != null)
			{
				if (!id.startsWith("add") || !id.endsWith("Form"))
					operation.getRequestBody().setRequired(true);
				if (operation.getRequestBody().getContent().containsKey("application/json")
						&& operation.getRequestBody().getContent().size() == 1)
					operation.getResponses().addApiResponse("415", new ApiResponse().description(
							"Content-Type must be application/json, including for bodyless form generation."));
			}
		}));
		setRequired(api, "RestProjectCreateRequest", "displayedName");
		setRequired(api, "RestProjectUpdateRequest", "displayedName");
		setRequired(api, "RestInvitationRequest", "emails", "expiration");
		setRequired(api, "RestPolicyDocumentRequest", "name", "displayedName", "mandatory", "contentType", "content");
		setRequired(api, "RestPolicyDocumentUpdateRequest", "id", "name", "displayedName", "mandatory", "contentType",
				"content");
		for (String type : List.of("RestPolicyDocument", "RestPolicyDocumentRequest", "RestPolicyDocumentUpdateRequest"))
			field(api, type, "contentType").setEnum(List.of("EMBEDDED", "LINK"));
		setRequired(api, "RestGroupCreateRequest", "displayedName");
		setRequired(api, "RestGroupUpdateRequest", "path", "displayedName", "public");
		setRequired(api, "RestAuthorizationRole", "role");
		for (String type : List.of("RestProject", "RestProjectId", "RestGroup", "RestGroupPath", "RestGroupMember",
				"RestProjectMembership", "RestInvitation", "RestInvitationOutcome", "RestRequest", "RestFormLink",
				"RestPolicyDocument", "RestPolicyDocumentId"))
		{
			Schema<?> model = api.getComponents().getSchemas().get(type);
			model.setRequired(List.copyOf(model.getProperties().keySet()));
		}
		field(api, "RestAuthorizationRole", "role").setEnum(List.of("regular", "manager", "projectsAdmin"));
		field(api, "RestInvitationOutcome", "status").setEnum(List.of("sent", "alreadyMember", "failed"));
		field(api, "RestRequest", "operation").setEnum(List.of("SignUp", "Update"));
		field(api, "RestRequest", "type").setEnum(List.of("Registration", "Enquiry"));
		for (String property : List.of("logoUrl", "registrationForm", "signUpEnquiry", "membershipUpdateEnquiry"))
			field(api, "RestProject", property).setNullable(true);
		for (String type : List.of("RestGroupMember", "RestRequest"))
			for (String property : List.of("name", "email"))
				field(api, type, property).setNullable(true);
		field(api, "RestProjectMembership", "email").setNullable(true);
		field(api, "RestInvitation", "lastSentTime").setNullable(true);
		field(api, "RestInvitation", "lastSentTime").setDescription(
				"Seconds since the Unix epoch, including fractional seconds; null until successfully sent.");
		field(api, "RestProjectCreateRequest", "projectId").setNullable(true);
		for (String type : List.of("RestProjectCreateRequest", "RestProjectUpdateRequest"))
			for (String property : List.of("logoUrl", "description", "readOnlyAttributes"))
				field(api, type, property).setNullable(true);
		field(api, "RestProject", "readOnlyAttributes").setNullable(true);
		field(api, "RestGroupDelegationRequest", "logoUrl").setNullable(true);
		field(api, "RestInvitationRequest", "groups").setNullable(true);
		field(api, "RestRequest", "emailVerified").setNullable(true);
		field(api, "RestInvitationOutcome", "message").setNullable(true);
		field(api, "RestGroup", "logoUrl").setNullable(true);
		field(api, "RestProjectCreateRequest", "projectId").setDescription(
				"Optional identifier; null or omission generates an identifier.");
		field(api, "RestProject", "urlId").setDescription(
				"Identifier for use unchanged in request paths; may be opaque for names containing percent signs.");
		field(api, "RestInvitationRequest", "expiration").setDescription(
				"Future expiration, as numeric Unix seconds or an ISO timestamp. Storage truncates to milliseconds.");
	}

	private static String tagFor(String route)
	{
		if (route.contains("/invitations"))
			return "Invitations";
		if (route.contains("/requests"))
			return "Requests";
		if (route.contains("/policyDocuments"))
			return "Policy documents";
		if (route.contains("/registrationForm") || route.contains("/signUpEnquiry")
				|| route.contains("/membershipUpdateEnquiry"))
			return "Forms";
		if (route.contains("/members"))
			return "Members";
		if (route.contains("/groups"))
			return "Subgroups";
		return "Projects";
	}

	private static void setRequired(OpenAPI api, String type, String... properties)
	{
		api.getComponents().getSchemas().get(type).setRequired(List.of(properties));
	}

	private static Schema field(OpenAPI api, String type, String property)
	{
		Schema<?> model = api.getComponents().getSchemas().get(type);
		return Objects.requireNonNull(model.getProperties().get(property), type + "." + property);
	}

	private static void addErrors(OpenAPI api)
	{
		api.getComponents().addSchemas("UpmanError", new ObjectSchema()
				.addProperty("message", new StringSchema()).addProperty("error", new StringSchema())
				.required(List.of("message", "error")));
		Map<String, String> errors = Map.of(
				"400", "Invalid input, rejected domain operation or missing direct authentication; Unity JSON error envelope.",
				"401", "Authentication is required by the deployed endpoint.",
				"403", "The caller is not authorized for this project or resource.",
				"404", "The requested resource is unavailable in this project; the body may be empty.",
				"406", "The requested response media type is not supported.",
				"500", "Server error; legacy null-input errors have an empty body.");
		errors.forEach((status, description) -> {
			ApiResponse response = new ApiResponse().description(description);
			if (Set.of("400", "403", "404").contains(status))
				response.content(new Content().addMediaType("application/json", new MediaType()
						.schema(new Schema<>().$ref("#/components/schemas/UpmanError"))));
			api.getComponents().addResponses("Upman" + status, response);
		});
	}

	private static String required(String key)
	{
		return Objects.requireNonNull(OPERATIONS.getProperty(key), "Missing OpenAPI description: " + key);
	}

	private static Properties loadOperations()
	{
		Properties descriptions = new Properties();
		try (var input = Objects.requireNonNull(UpmanOpenApiDescriptions.class
				.getResourceAsStream("openapi/operations.properties"));
				var reader = new InputStreamReader(input, StandardCharsets.UTF_8))
		{
			descriptions.load(reader);
			return descriptions;
		} catch (IOException e)
		{
			throw new IllegalStateException("Cannot load UpMan OpenAPI descriptions", e);
		}
	}
}
