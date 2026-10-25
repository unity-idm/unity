package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class UpmanPolicyHttpContractTest extends UpmanHttpContractTestBase
{
	private static final String PROJECTS = "/restupm/v1/projects";
	private static final String POLICIES = PROJECTS + "/contract/policyDocuments";
	private static final String DENIED = """
			{"message":"Access to policy document is denied. The policy document is not in project scope.",
			 "error":"AuthorizationException"}
			""";

	@Test
	void shouldPreservePolicyTreesAndRevisionBehaviorAcrossCrud() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		String create = fixture("policy-create.json");

		HttpResult added = exchange("POST", POLICIES, create, ContentType.APPLICATION_JSON);

		assertThat(added.status()).as(added.body()).isEqualTo(200);
		assertThat(added.contentType()).startsWith("application/json");
		JsonNode id = m.readTree(added.body());
		assertThat(id.properties()).hasSize(1);
		assertThat(id.path("id").isIntegralNumber()).isTrue();
		String policy = POLICIES + "/" + id.get("id").asLong();
		ObjectNode expected = (ObjectNode) m.readTree(create);
		expected.set("id", id.get("id"));
		expected.put("revision", 1);
		assertJson(exchange("GET", policy), 200, expected);
		assertJson(exchange("GET", POLICIES), 200, m.createArrayNode().add(expected));

		ObjectNode update = expected.deepCopy();
		update.remove("revision");
		update.put("mandatory", false);
		update.putObject("content").put("en", "New terms");
		for (String query : new String[] {"", "?incrementRevision=false", "?incrementRevision=true"})
		{
			assertEmpty(exchange("PUT", POLICIES + query, update.toString(), ContentType.APPLICATION_JSON), 204);
			expected = update.deepCopy();
			expected.put("revision", query.endsWith("=true") ? 2 : 1);
			assertJson(exchange("GET", policy), 200, expected);
		}

		assertEmpty(exchange("DELETE", policy), 204);
		assertJson(exchange("GET", POLICIES), 200, "[]");
		assertJson(exchange("GET", policy), 403, DENIED);
	}

	@Test
	void shouldDenyForeignProjectPolicyReadsAndMutations() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		ObjectNode sibling = (ObjectNode) m.readTree(fixture("project-create.json"));
		sibling.put("projectId", "sibling");
		exchange("POST", PROJECTS, sibling.toString(), ContentType.APPLICATION_JSON);
		HttpResult added = exchange("POST", POLICIES, fixture("policy-create.json"), ContentType.APPLICATION_JSON);
		JsonNode id = m.readTree(added.body()).get("id");
		String foreign = PROJECTS + "/sibling/policyDocuments";

		assertJson(exchange("GET", foreign + "/" + id.asLong()), 403, DENIED);
		assertJson(exchange("DELETE", foreign + "/" + id.asLong()), 403, DENIED);
		ObjectNode update = (ObjectNode) m.readTree(fixture("policy-create.json"));
		update.set("id", id);
		assertJson(exchange("PUT", foreign, update.toString(), ContentType.APPLICATION_JSON), 403, DENIED);

		ObjectNode expected = update.deepCopy();
		expected.put("revision", 1);
		assertJson(exchange("GET", POLICIES + "/" + id.asLong()), 200, expected);
		assertJson(exchange("GET", foreign), 200, "[]");
		assertThat(exchange("GET", PROJECTS + "/missing/policyDocuments").status()).isEqualTo(404);
	}
}
