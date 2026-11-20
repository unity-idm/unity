package io.imunity.upman.rest;

import java.util.List;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.EntityParam;

class UpmanInputHttpContractTest extends UpmanHttpContractTestBase
{
	private static final String PROJECTS = "/restupm/v1/projects";
	private static final String PROJECT = PROJECTS + "/contract";

	@Test
	void shouldPreserveAuthorizationErrorsWithoutChangingStoredProjects() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		attrsMan.removeAttribute(new EntityParam(entityId), "/A", "sys:ProjectManagementRESTAPIRole");
		String denied = """
				{"message":"Access is denied. The operation requires project management REST API role",
				 "error":"AuthorizationException"}
				""";

		assertJson(exchange("GET", PROJECT), 403, denied);
		assertJson(exchange("DELETE", PROJECT), 403, denied);
		assertJson(exchange("GET", PROJECT + "/policyDocuments"), 403, denied);
		assertJson(exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON), 403, denied);

		attrsMan.createAttribute(new EntityParam(entityId), new Attribute(
				"sys:ProjectManagementRESTAPIRole", "enumeration", "/A", List.of("manager")));
		assertJson(exchange("GET", PROJECT), 200, fixture("project.json"));
		assertJson(exchange("GET", PROJECTS), 200, m.createArrayNode().add(m.readTree(fixture("project.json"))));
	}

	@Test
	void shouldPreserveProjectScalarCoercionsAndDefaults() throws Exception
	{
		String body = """
				{"projectId":"contract","displayedName":{"en":123},"description":{},
				 "public":"false","enableSubprojects":1}
				""";

		assertJson(exchange("POST", PROJECTS, body, ContentType.APPLICATION_JSON), 200, "{\"id\":\"contract\"}");

		assertJson(exchange("GET", PROJECT), 200, """
				{"projectId":"contract","urlId":"contract","displayedName":{"en":"123"},"description":{},
				 "public":false,"enableSubprojects":true,"logoUrl":null,"readOnlyAttributes":null,
				 "registrationForm":null,"signUpEnquiry":null,"membershipUpdateEnquiry":null}
				""");
	}

	@Test
	void shouldMapFormBuilderFailuresToTheLegacyErrorEnvelope() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		String error = """
				{"message":"Can't perform JSON deserialization","error":"BadRequestException"}
				""";

		for (String path : new String[] {"registrationForm", "signUpEnquiry", "membershipUpdateEnquiry"})
		{
			assertJson(exchange("POST", PROJECT + "/" + path, "{}", ContentType.APPLICATION_JSON), 400, error);
			assertJson(exchange("POST", PROJECT + "/" + path + "?autogenerate=false", "{",
					ContentType.APPLICATION_JSON), 400, error);
		}
		assertJson(exchange("GET", PROJECT), 200, fixture("project.json"));
	}

	@Test
	void shouldPreserveBooleanQueryCoercionAndInvalidNumericQueryErrors() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		ObjectNode form = (ObjectNode) m.readTree(fixture("enquiry-form-create.json"));

		assertEmpty(exchange("POST", PROJECT + "/signUpEnquiry?autogenerate=invalid", form.toString(),
				ContentType.APPLICATION_JSON), 204);
		assertJson(exchange("GET", PROJECT + "/signUpEnquiry"), 200, form);
		assertEmpty(exchange("GET", PROJECT + "/members/by-id/not-a-number"), 404);
	}
}
