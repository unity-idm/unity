package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.Instant;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;

class UpmanInvitationHttpContractTest extends UpmanHttpContractTestBase
{
	private static final String PROJECTS = "/restupm/v1/projects";
	private static final String PROJECT = PROJECTS + "/contract";
	private static final String INVITATIONS = PROJECT + "/invitations";
	private static final Instant EXPIRATION = Instant.parse("2035-01-01T00:00:00.125Z");

	@Autowired
	private InvitationManagement invitations;
	@Autowired
	private PublicRegistrationURLSupport publicUrls;

	@Test
	void shouldPreserveInvitationOutcomesTreesAndNumericTimestamps() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		assertEmpty(exchange("POST", PROJECT + "/registrationForm?autogenerate=true", "{ignored",
				ContentType.APPLICATION_JSON), 204);
		assertEmpty(exchange("POST", PROJECT + "/signUpEnquiry?autogenerate=true", "", ContentType.APPLICATION_JSON), 204);
		assertEmpty(exchange("POST", PROJECT + "/members/" + entityEmail), 204);
		String body = fixture("invitation-create.json");

		assertJson(exchange("POST", INVITATIONS, body, ContentType.APPLICATION_JSON), 201, """
				[{"email":"invitee@example.com","status":"sent","message":null},
				 {"email":"email@email.com","status":"alreadyMember","message":null}]
				""");
		HttpResult list = exchange("GET", INVITATIONS);
		assertThat(list.status()).as(list.body()).isEqualTo(200);
		JsonNode tree = m.readTree(list.body());
		assertThat(tree).hasSize(1);
		String code = tree.get(0).get("code").asText();
		ObjectNode expected = expectedInvitation(code);
		assertJson(list, 200, m.createArrayNode().add(expected));
		assertJson(exchange("GET", INVITATIONS + "/" + code), 200, expected);
		assertEmpty(exchange("POST", INVITATIONS + "/" + code + "/resend"), 204);
		expected = expectedInvitation(code);
		assertThat(expected.path("numberOfSends").asInt()).isEqualTo(2);
		assertJson(exchange("GET", INVITATIONS + "/" + code), 200, expected);
		assertEmpty(exchange("POST", INVITATIONS + "/" + code + "/reinvite"), 204);
		JsonNode replacement = m.readTree(exchange("GET", INVITATIONS).body());
		assertThat(replacement).hasSize(1);
		String newCode = replacement.get(0).get("code").asText();
		assertThat(newCode).isNotEqualTo(code);
		assertThat(exchange("GET", INVITATIONS + "/" + code).status()).isEqualTo(404);
		assertEmpty(exchange("DELETE", INVITATIONS + "/" + newCode), 204);
		assertJson(exchange("GET", INVITATIONS), 200, "[]");
	}

	@Test
	void shouldAcceptNumericExpirationAndKeepSelectedGroupsAndProjectIsolation() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		var sibling = (ObjectNode) m.readTree(fixture("project-create.json"));
		sibling.put("projectId", "other");
		exchange("POST", PROJECTS, sibling.toString(), ContentType.APPLICATION_JSON);
		groupsMan.addGroup(new Group("/A/contract/team"));
		assertEmpty(exchange("POST", PROJECT + "/registrationForm?autogenerate=true", "",
				ContentType.APPLICATION_JSON), 204);
		assertEmpty(exchange("POST", PROJECT + "/signUpEnquiry?autogenerate=true", "",
				ContentType.APPLICATION_JSON), 204);
		var input = (ObjectNode) m.readTree(fixture("invitation-create.json"));
		input.putArray("emails").add("invitee@example.com");
		input.putArray("groups").add("team");
		input.put("allowModifyGroups", true);

		for (BigDecimal expiration : new BigDecimal[] {new BigDecimal("2051222400"),
				new BigDecimal("2051222400.125999999")})
		{
			input.put("expiration", expiration);
			assertJson(exchange("POST", INVITATIONS, input.toString(), ContentType.APPLICATION_JSON), 201,
					"[{\"email\":\"invitee@example.com\",\"status\":\"sent\",\"message\":null}]");
			JsonNode list = m.readTree(exchange("GET", INVITATIONS).body());
			assertThat(list).hasSize(1);
			String code = list.get(0).path("code").asText();
			var stored = invitations.getInvitation(code);
			var expected = (ObjectNode) m.readTree(fixture("invitation.json"));
			expected.put("code", code);
			expected.putArray("groups").add("team");
			expected.put("allowModifyGroups", true);
			expected.put("expiration", BigDecimal.valueOf(stored.getInvitation().getExpiration().toEpochMilli(), 3));
			assertThat(stored.getInvitation().getExpiration().toEpochMilli()).isEqualTo(
					expiration.movePointRight(3).longValue());
			expected.put("lastSentTime", BigDecimal.valueOf(stored.getLastSentTime().toEpochMilli(), 3));
			expected.put("numberOfSends", stored.getNumberOfSends());
			expected.put("link", publicUrls.getPublicRegistrationLink(stored.getInvitation().getFormsPrefillData()
					.getFirst().getFormId(), code));
			assertJson(exchange("GET", INVITATIONS + "/" + code), 200, expected);
			String foreign = PROJECTS + "/other/invitations/" + code;
			assertEmpty(exchange("GET", foreign), 404);
			assertEmpty(exchange("DELETE", foreign), 404);
			assertJson(exchange("GET", INVITATIONS + "/" + code), 200, expected);
			assertEmpty(exchange("DELETE", INVITATIONS + "/" + code), 204);
		}
	}

	@Test
	void shouldReturnPerEmailFailuresAndContinueProcessingTheBatch() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		assertEmpty(exchange("POST", PROJECT + "/members/" + entityEmail), 204);
		var input = (ObjectNode) m.readTree(fixture("invitation-create.json"));
		input.putArray("emails").add("first@example.com").add(entityEmail).add("last@example.com");

		assertJson(exchange("POST", INVITATIONS, input.toString(), ContentType.APPLICATION_JSON), 201, """
				[{"email":"first@example.com","status":"failed","message":"Misconfigured project group /A/contract"},
				 {"email":"email@email.com","status":"alreadyMember","message":null},
				 {"email":"last@example.com","status":"failed","message":"Misconfigured project group /A/contract"}]
				""");
		assertJson(exchange("GET", INVITATIONS), 200, "[]");
	}

	private ObjectNode expectedInvitation(String code) throws Exception
	{
		var stored = invitations.getInvitation(code);
		ObjectNode expected = (ObjectNode) m.readTree(fixture("invitation.json"));
		assertThat(stored.getInvitation().getExpiration()).isEqualTo(EXPIRATION);
		expected.put("code", code);
		expected.put("lastSentTime", BigDecimal.valueOf(stored.getLastSentTime().toEpochMilli(), 3));
		expected.put("numberOfSends", stored.getNumberOfSends());
		expected.put("link", publicUrls.getPublicRegistrationLink(stored.getInvitation().getFormsPrefillData()
				.getFirst().getFormId(), code));
		return expected;
	}
}
