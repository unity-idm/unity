package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class UpmanGroupHttpContractTest extends UpmanHttpContractTestBase
{
	private static final String PROJECTS = "/restupm/v1/projects";
	private static final String GROUPS = PROJECTS + "/contract/groups";

	@Test
	void shouldPreserveGroupTreesCreatedStatusAndDelegation() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		HttpResult created = exchange("POST", GROUPS, """
				{"parentPath":"","displayedName":{"en":"Team ☃"},"public":false}
				""", ContentType.APPLICATION_JSON);
		assertThat(created.status()).as(created.body()).isEqualTo(201);
		assertThat(created.contentType()).startsWith("application/json");
		JsonNode id = m.readTree(created.body());
		assertThat(id.properties()).hasSize(1);
		String path = id.path("path").asText();
		assertThat(path).isNotBlank();
		String group = GROUPS + "/by-path?path=" + path;
		ObjectNode expected = (ObjectNode) m.readTree("""
				{"path":"","displayedName":{"en":"Team ☃"},"public":false,"delegated":false,
				 "enableSubprojects":false,"logoUrl":null,"subGroups":[]}
				""");
		expected.put("path", path);
		ObjectNode root = (ObjectNode) m.readTree("""
				{"path":"","displayedName":{"en":"Project żółć 日本語"},"public":false,"delegated":true,
				 "enableSubprojects":true,"logoUrl":null,"subGroups":[]}
				""");
		root.putArray("subGroups").add(path);
		assertJson(exchange("GET", group), 200, expected);
		assertJson(exchange("GET", GROUPS), 200, m.createArrayNode().add(root).add(expected));
		assertEmpty(exchange("PUT", GROUPS, """
				{"path":"%s","displayedName":{"en":"Renamed"},"public":false}
				""".formatted(path), ContentType.APPLICATION_JSON), 204);
		expected.putObject("displayedName").put("en", "Renamed");
		assertJson(exchange("GET", group), 200, expected);
		assertEmpty(exchange("PUT", GROUPS + "/by-path/delegation?path=" + path, """
				{"enabled":true,"enableSubprojects":false,"logoUrl":"/team.png"}
				""", ContentType.APPLICATION_JSON), 204);
		expected.put("delegated", true);
		expected.put("logoUrl", "/team.png");
		assertJson(exchange("GET", group), 200, expected);
		assertEmpty(exchange("DELETE", group), 204);
		root.putArray("subGroups");
		assertJson(exchange("GET", GROUPS), 200, m.createArrayNode().add(root));
		assertThat(exchange("GET", group).status()).isEqualTo(404);
	}
}
