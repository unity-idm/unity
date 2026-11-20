package io.imunity.upman.rest;

import java.util.List;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.group.Group;

class UpmanMemberHttpContractTest extends UpmanHttpContractTestBase
{
	private static final String PROJECTS = "/restupm/v1/projects";
	private static final String MEMBERS = PROJECTS + "/contract/members";

	@Test
	void shouldPreservePopulatedNamesAttributesAndNullEmailMembers() throws Exception
	{
		AttributeType name = new AttributeType("cn", "string");
		name.setMinElements(1);
		name.setMaxElements(1);
		name.getMetadata().put(EntityNameMetadataProvider.NAME, "");
		aTypeMan.addAttributeType(name);
		AttributeType organization = new AttributeType("o", "string");
		organization.setMaxElements(10);
		aTypeMan.addAttributeType(organization);
		var create = (com.fasterxml.jackson.databind.node.ObjectNode) m.readTree(fixture("project-create.json"));
		create.putArray("readOnlyAttributes").add("o");
		exchange("POST", PROJECTS, create.toString(), ContentType.APPLICATION_JSON);
		attrsMan.createAttribute(new EntityParam(entityId), new Attribute("cn", "string", "/", List.of("Żółć 日本語")));
		assertEmpty(exchange("PUT", MEMBERS + "/by-id/" + entityId), 204);
		attrsMan.createAttribute(new EntityParam(entityId),
				new Attribute("o", "string", "/A/contract", List.of("Organization", "Second")));
		JsonNode attributes = m.readTree("""
				[{"name":"o","values":["Organization","Second"]}]
				""");
		var emailTree = m.createObjectNode().put("email", entityEmail).put("role", "regular");
		emailTree.set("attributes", attributes);
		JsonNode memberTree = m.readTree(emailTree.deepCopy().put("entityId", entityId)
				.put("name", "Żółć 日本語").toString());
		assertJson(exchange("GET", MEMBERS + "/" + entityEmail), 200, emailTree);
		assertJson(exchange("GET", MEMBERS), 200, m.createArrayNode().add(emailTree));
		assertJson(exchange("GET", MEMBERS + "?groupPath="), 200, m.createArrayNode().add(memberTree));
		assertJson(exchange("GET", MEMBERS + "/by-id/" + entityId), 200, memberTree);

		assertEmpty(exchange("DELETE", MEMBERS + "/by-id/" + entityId), 204);
		var user = createUsernameUser("without-email", null, DEF_PASSWORD, CRED_REQ_PASS);
		assertEmpty(exchange("PUT", MEMBERS + "/by-id/" + user.getEntityId()), 204);
		JsonNode nullEmail = m.readTree("""
				{"entityId":%d,"name":null,"email":null,"role":"regular","attributes":[]}
				""".formatted(user.getEntityId()));
		assertJson(exchange("GET", MEMBERS + "/by-id/" + user.getEntityId()), 200, nullEmail);
		assertJson(exchange("GET", MEMBERS + "?groupPath="), 200, m.createArrayNode().add(nullEmail));
		assertJson(exchange("GET", MEMBERS), 200, """
				[{"email":null,"role":"regular","attributes":[]}]
				""");
	}

	@Test
	void shouldPreserveEmailAndEntityIdMemberRepresentations() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		String emailMember = MEMBERS + "/" + entityEmail;
		String idMember = MEMBERS + "/by-id/" + entityId;
		JsonNode emailTree = m.readTree("""
				{"email":"email@email.com","role":"regular","attributes":[]}
				""");
		JsonNode idTree = m.readTree("""
				{"entityId":%d,"name":null,"email":"email@email.com","role":"regular","attributes":[]}
				""".formatted(entityId));

		assertEmpty(exchange("POST", emailMember), 204);
		assertJson(exchange("GET", emailMember), 200, emailTree);
		assertJson(exchange("GET", MEMBERS), 200, m.createArrayNode().add(emailTree));
		assertJson(exchange("GET", MEMBERS + "?groupPath="), 200, m.createArrayNode().add(idTree));
		assertJson(exchange("GET", idMember), 200, idTree);
		assertJson(exchange("GET", idMember + "?groupPath="), 200, idTree);
		assertJson(exchange("GET", emailMember + "/role"), 200, "{\"role\":\"regular\"}");
		assertJson(exchange("GET", idMember + "/role"), 200, "{\"role\":\"regular\"}");
		assertEmpty(exchange("DELETE", emailMember), 204);
		assertJson(exchange("GET", MEMBERS), 200, "[]");
		assertEmpty(exchange("PUT", idMember), 204);
		assertJson(exchange("GET", emailMember), 200, emailTree);
		assertEmpty(exchange("DELETE", idMember), 204);
		assertJson(exchange("GET", MEMBERS + "?groupPath="), 200, "[]");
		assertEmpty(exchange("PUT", idMember), 204);
		assertEmpty(exchange("PUT", emailMember + "/role", "{\"role\":\"manager\"}", ContentType.TEXT_PLAIN), 204);
		assertJson(exchange("GET", idMember + "/role"), 200, "{\"role\":\"manager\"}");
		assertJson(exchange("PUT", idMember + "/role", "{\"role\":\"regular\"}",
				ContentType.APPLICATION_JSON), 409, """
				{"message":"At least one manager must remain in group /A/contract",
				 "error":"OneManagerRemainsException"}
				""");
		assertJson(exchange("GET", emailMember + "/role"), 200, "{\"role\":\"manager\"}");
		assertEmpty(exchange("PUT", idMember + "/role", "{\"role\":\"projectsAdmin\"}",
				ContentType.APPLICATION_JSON), 204);
		assertJson(exchange("GET", emailMember + "/role"), 200, "{\"role\":\"projectsAdmin\"}");
	}

	@Test
	void shouldKeepSubgroupMembershipSeparateFromProjectMembership() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		groupsMan.addGroup(new Group("/A/contract/team"));
		String member = MEMBERS + "/by-id/" + entityId;
		JsonNode expected = m.readTree("""
				{"entityId":%d,"name":null,"email":"email@email.com","role":"regular","attributes":[]}
				""".formatted(entityId));
		assertEmpty(exchange("PUT", member + "?groupPath=team"), 204);

		assertJson(exchange("GET", member + "?groupPath=team"), 200, expected);
		assertJson(exchange("GET", MEMBERS + "?groupPath=team"), 200, m.createArrayNode().add(expected));
		assertJson(exchange("GET", MEMBERS), 200, m.createArrayNode().add(m.readTree("""
				{"email":"email@email.com","role":"regular","attributes":[]}
				""")));
		assertEmpty(exchange("DELETE", member + "?groupPath=team"), 204);
		assertJson(exchange("GET", MEMBERS + "?groupPath=team"), 200, "[]");
		assertJson(exchange("GET", member), 200, expected);
	}
}
