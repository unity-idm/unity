package io.imunity.rest.admin;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class AdminOpenApiModels
{
	static void apply(OpenAPI api)
	{
		var schemas = api.getComponents().getSchemas();
		schemas.put("ResolvedEndpoint", ref("RestResolvedEndpoint"));
		schemas.remove("ObjectNode");
		schemas.put("JsonNode", new Schema<>().description(
				"Arbitrary JSON supplied by the identity type or attribute syntax plugin, including null."));
		nullable(api, "RestEndpointConfiguration", "displayedName", "description", "configuration");
		nullable(api, "RestAuthenticationRealm", "description");
		List<Schema> invitations = new ArrayList<>();
		for (String kind : List.of("Registration", "Enquiry", "Combo"))
		{
			String parameterType = "Rest" + kind + "InvitationParam";
			Schema parameter = schemas.get(parameterType);
			parameter.setRequired(List.of("type", "expiration"));
			nullable(api, parameterType, "contactAddress", "inviter");
			parameter.addProperty("type", new StringSchema()._enum(List.of(
					kind.equals("Combo") ? "COMBO" : kind.toUpperCase(java.util.Locale.ROOT))));
			Schema metadata = new ObjectSchema().addProperty("registrationCode", new StringSchema())
					.addProperty("numberOfSends", new IntegerSchema())
					.addProperty("creationTime", milliseconds()).addProperty("lastSentTime", milliseconds())
					.required(List.of("registrationCode", "numberOfSends"));
			String name = "Admin" + kind + "Invitation";
			schemas.put(name, new Schema<>().allOf(List.of(ref(parameterType), metadata)));
			invitations.add(ref(name));
		}
		schemas.put("RestInvitationWithCode", new Schema<>().anyOf(invitations)
				.description("Flattened invitation. All response timestamps are Unix milliseconds."));
		response(api, "/v1/invitation/{code}", ref("RestInvitationWithCode"));
		response(api, "/v1/invitations", new ArraySchema().items(ref("RestInvitationWithCode")));
		Schema request = new Schema<>().anyOf(List.of(ref("RestRegistrationInvitationParam"),
				ref("RestEnquiryInvitationParam")));
		api.getPaths().get("/v1/invitation").getPost().getRequestBody().getContent()
				.get("application/json").setSchema(request);
		api.getPaths().get("/v1/invitation/{code}").getPut().getRequestBody().getContent()
				.get("application/json").setSchema(request);
		schemas.get("RestInvitationParam").setRequired(List.of("type", "expiration"));
		propertiesWith(schemas.get("RestInvitationParam"), "expiration").get("expiration").setDescription(
				"Expiration in milliseconds since the Unix epoch.");
		nullable(api, "RestInvitationParam", "contactAddress", "inviter");
		nullable(api, "RestRegistrationInvitationParam", "expectedIdentity");
		nullable(api, "RestFormattedToken", "owner", "created", "expires");
		schemas.get("RestFormattedToken").addProperty("contents", new Schema<>()
				.description("Arbitrary JSON produced by the token type's formatter, including scalar values or null."));
		schemas.get("RestFormattedToken").setRequired(List.of("type", "value", "owner", "created", "expires", "contents"));
		nullable(api, "RestGroup", "description");
		nullable(api, "RestGroupDelegationConfiguration", "logoUrl", "registrationForm", "signupEnquiryForm",
				"membershipUpdateEnquiryForm", "attributes", "policyDocumentsIds");
		for (String type : List.of("RestRegistrationRequestState", "RestEnquiryResponseState"))
		{
			nullable(api, type, "Comments", "UserLocale", "RegistrationCode");
			var required = new ArrayList<String>(schemas.get(type).getProperties().keySet());
			required.remove("CreatedEntityId");
			schemas.get(type).setRequired(required);
		}
		schemas.get("RestEntityId").setRequired(List.of("entityId"));
		schemas.get("RestEntity").setRequired(List.of("entityInformation", "identities", "credentialInfo"));
		schemas.get("RestEntityInformation").setRequired(List.of("entityId", "state"));
		for (String type : List.of("RestPolicyDocument", "RestPolicyDocumentId"))
			schemas.get(type).setRequired(List.copyOf(schemas.get(type).getProperties().keySet()));
		for (String field : List.of("displayedName", "content"))
			propertiesWith(schemas.get("RestPolicyDocumentRequest"), field).get(field).setMinProperties(1);
		propertiesWith(schemas.get("RestPolicyDocumentRequest"), "contentType").get("contentType")
				.setEnum(List.of("EMBEDDED", "LINK"));
	}

	private static Schema milliseconds()
	{
		return new IntegerSchema().format("int64").description("Milliseconds since the Unix epoch");
	}

	private static Schema ref(String name)
	{
		return new Schema<>().$ref("#/components/schemas/" + name);
	}

	private static void response(OpenAPI api, String path, Schema schema)
	{
		api.getPaths().get(path).getGet().getResponses().get("200").getContent()
				.get("application/json").setSchema(schema);
	}

	private static void nullable(OpenAPI api, String type, String... names)
	{
		Schema model = api.getComponents().getSchemas().get(type);
		for (String name : names)
		{
			Map<String, Schema> properties = propertiesWith(model, name);
			Schema property = properties.get(name);
			if (property.get$ref() == null)
				property.setNullable(true);
			else
				properties.put(name, new Schema<>().anyOf(List.of(property,
						new ObjectSchema().nullable(true)._enum(Collections.singletonList(null)))));
		}
	}

	private static Map<String, Schema> propertiesWith(Schema model, String name)
	{
		if (model.getProperties() != null && model.getProperties().containsKey(name))
			return model.getProperties();
		if (model.getAllOf() != null)
			for (Object part : model.getAllOf())
			{
				Schema schema = (Schema) part;
				if (schema.getProperties() != null && schema.getProperties().containsKey(name))
					return schema.getProperties();
			}
		throw new IllegalStateException("Missing admin property: " + name);
	}
}
