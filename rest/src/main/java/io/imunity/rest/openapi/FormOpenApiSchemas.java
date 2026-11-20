package io.imunity.rest.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Form DTOs have the same public properties in both proven endpoint mapper configurations. */
public final class FormOpenApiSchemas
{
	public static void apply(OpenAPI api)
	{
		describeLayoutSubtypes(api);
		setRequired(api, "RestRegistrationForm", "Name", "DefaultCredentialRequirement");
		setRequired(api, "RestEnquiryForm", "Name", "type");
		for (String type : List.of("RestEnquiryForm", "RestRegistrationForm"))
			field(api, type, "FormInformation").setNullable(true);
		for (String property : List.of("label", "description"))
			field(api, "RestGroupRegistrationParam", property).setNullable(true);
		for (String type : List.of("RestEnquiryFormNotifications", "RestRegistrationFormNotifications"))
			for (String property : List.of("rejectedTemplate", "acceptedTemplate", "updatedTemplate",
					"invitationTemplate", "invitationProcessedTemplate", "adminsNotificationGroup", "submittedTemplate"))
				field(api, type, property).setNullable(true);
		field(api, "RestEnquiryFormNotifications", "enquiryToFillTemplate").setNullable(true);
		for (String property : List.of("SignInLink", "AutoLoginToRealm"))
			field(api, "RestRegistrationForm", property).setNullable(true);
		field(api, "RestI18nString", "DefaultValue").setNullable(true);
		nullableReference(api, "RestRegistrationForm", "FormInformation2ndStage");
		nullableReference(api, "RestRegistrationForm", "SwitchToEnquiryInfo");
	}

	private static void describeLayoutSubtypes(OpenAPI api)
	{
		Map<String, String> subtypes = Map.of(
				"BasicFormElement", "RestBasicFormElement",
				"FormCaptionElement", "RestFormCaptionElement",
				"FormParameterElement", "RestFormParameterElement",
				"FormSeparatorElement", "RestFormSeparatorElement",
				"FormLocalSignupButtonElement", "RestFormLocalSignupButtonElement");
		var mapping = new java.util.TreeMap<String, String>();
		List<Schema> alternatives = new java.util.ArrayList<>();
		subtypes.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
			String clazz = "pl.edu.icm.unity.types.registration.layout." + entry.getKey();
			String reference = "#/components/schemas/" + entry.getValue();
			mapping.put(clazz, reference);
			alternatives.add(new Schema<>().$ref(reference));
			api.getComponents().getSchemas().get(entry.getValue()).addProperty("clazz",
					new StringSchema()._enum(List.of(clazz)));
		});
		setRequired(api, "RestFormElement", "clazz");
		var discriminator = new io.swagger.v3.oas.models.media.Discriminator().propertyName("clazz").mapping(mapping);
		api.getComponents().getSchemas().get("RestFormElement").setDiscriminator(discriminator);
		field(api, "RestFormLayout", "elements").setItems(
				new Schema<>().oneOf(alternatives).discriminator(discriminator));
	}

	private static void nullableReference(OpenAPI api, String type, String property)
	{
		Schema original = field(api, type, property);
		api.getComponents().getSchemas().get(type).addProperty(property, new Schema<>().anyOf(List.of(original,
				new ObjectSchema().nullable(true)._enum(java.util.Collections.singletonList(null)))));
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

}
