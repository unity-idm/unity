package io.imunity.upman.rest;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;

final class UpmanOpenApiExamples
{
	static void apply(OpenAPI api)
	{
		Map<String, String> inputs = Map.ofEntries(
				Map.entry("addProject", "project-create.json"),
				Map.entry("updateProject", "project-update.json"),
				Map.entry("addPolicyDocument", "policy-create.json"),
				Map.entry("sendInvitations", "invitation-create.json"),
				Map.entry("addRegistrationForm", "registration-form-create.json"),
				Map.entry("updateRegistrationForm", "registration-form-create.json"),
				Map.entry("addSignupEnquiryForm", "enquiry-form-create.json"),
				Map.entry("updateSignupEnquiryForm", "enquiry-form-create.json"),
				Map.entry("addMembershipUpdateEnquiryForm", "membership-form-create.json"),
				Map.entry("updateMembershipUpdateEnquiryForm", "membership-form-create.json"));
		Map<String, JsonNode> outputs = Map.ofEntries(
				Map.entry("getProject", fixture("project.json")),
				Map.entry("getProjects", array(fixture("project.json"))),
				Map.entry("getRequest", fixture("request.json")),
				Map.entry("getRequests", array(fixture("request.json"))),
				Map.entry("getRegistrationForm", registrationForm()),
				Map.entry("getSignupEnquiryForm", fixture("enquiry-form-create.json")),
				Map.entry("getMembershipUpdateEnquiryForm", fixture("membership-form-create.json")));
		api.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
			String id = operation.getOperationId();
			if (inputs.containsKey(id))
				operation.getRequestBody().getContent().values().forEach(media ->
						media.setExample(fixture(inputs.get(id))));
			if (id.equals("getProjectMembers"))
				operation.getResponses().get("200").getContent().values().forEach(media -> media.setExamples(Map.of(
						"emailMembers", new Example().summary("groupPath omitted").value(fixture("members-email.json")),
						"entityMembers", new Example().summary("groupPath supplied, including empty")
								.value(fixture("members-by-id.json")),
						"empty", new Example().summary("Either representation with no members")
								.value(JsonNodeFactory.instance.arrayNode()))));
			if (id.equals("getInvitation") || id.equals("getInvitations"))
			{
				boolean list = id.equals("getInvitations");
				operation.getResponses().get("200").getContent().values().forEach(media -> media.setExamples(Map.of(
						"sent", new Example().value(list ? array(fixture("invitation.json")) : fixture("invitation.json")),
						"unsent", new Example().summary("Saved before a successful send").value(list
								? array(fixture("invitation-unsent.json")) : fixture("invitation-unsent.json")))));
			}

			if (outputs.containsKey(id))
				operation.getResponses().get("200").getContent().values().forEach(media ->
						media.setExample(outputs.get(id)));
		}));
	}

	private static JsonNode array(JsonNode value)
	{
		return JsonNodeFactory.instance.arrayNode().add(value);
	}

	private static JsonNode registrationForm()
	{
		ObjectNode form = (ObjectNode) fixture("registration-form-create.json");
		form.putObject("FormInformation2ndStage").putNull("DefaultValue").putObject("Map");
		return form;
	}

	private static JsonNode fixture(String name)
	{
		try (var input = Objects.requireNonNull(UpmanOpenApiExamples.class.getResourceAsStream("contract/" + name)))
		{
			return UpmanJsonProvider.newMapper().readTree(input);
		} catch (IOException e)
		{
			throw new IllegalStateException("Cannot load OpenAPI example " + name, e);
		}
	}
}
