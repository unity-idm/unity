package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;

class UpmanRequestHttpContractTest extends UpmanHttpContractTestBase
{
	private static final String PROJECTS = "/restupm/v1/projects";
	private static final String PROJECT = PROJECTS + "/contract";
	private static final String REQUESTS = PROJECT + "/requests";

	@Test
	void shouldPreservePendingRequestTreesAndBothDecisions() throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		var sibling = (ObjectNode) m.readTree(fixture("project-create.json"));
		sibling.put("projectId", "other");
		exchange("POST", PROJECTS, sibling.toString(), ContentType.APPLICATION_JSON);
		ObjectNode form = (ObjectNode) m.readTree(fixture("registration-form-create.json"));
		form.putArray("IdentityParams").addObject().put("identityType", "email")
				.put("retrievalSettings", "interactive").put("confirmationMode", "ON_SUBMIT").put("optional", false);
		assertEmpty(exchange("POST", PROJECT + "/registrationForm", form.toString(), ContentType.APPLICATION_JSON), 204);

		for (String decision : new String[] {"accept", "decline", "drop"})
		{
			String email = decision + "@example.com";
			var request = new RegistrationRequestBuilder().withFormId("contract-form")
					.withRegistrationCode("123").withAddedIdentity(new IdentityParam(EmailIdentity.ID, email))
					.withAddedAgreement().withSelected(false).endAgreement()
					.withAddedGroupSelection().withGroup("/A/contract").endGroupSelection().build();
			String id = registrationsMan.submitRegistrationRequest(request,
					new RegistrationContext(false, TriggeringMode.manualAtLogin));
			ObjectNode expected = (ObjectNode) m.readTree(fixture("request.json"));
			expected.put("id", id);
			expected.put("email", email);
			expected.put("requestedTime", BigDecimal.valueOf(
					registrationsMan.getRegistrationRequest(id).getTimestamp().getTime(), 3));
			assertJson(exchange("GET", REQUESTS + "/" + id), 200, expected);
			assertJson(exchange("GET", REQUESTS), 200, m.createArrayNode().add(expected));
			String foreign = PROJECTS + "/other/requests/" + id;
			assertEmpty(exchange("GET", foreign), 404);
			assertEmpty(exchange("POST", foreign + "/state?decision=decline"), 404);
			assertJson(exchange("GET", REQUESTS + "/" + id), 200, expected);
			assertUpdateFlagsPreserveRequest(form, id, expected);

			if (decision.equals("drop"))
			{
				assertDeleteFlagsAndRestoreDetachedForm(id, expected);
			} else
				assertEmpty(exchange("POST", REQUESTS + "/" + id + "/state?decision=" + decision), 204);

			assertJson(exchange("GET", REQUESTS), 200, "[]");
			assertThat(exchange("GET", REQUESTS + "/" + id).status()).isEqualTo(404);
		}
	}

	private void assertUpdateFlagsPreserveRequest(ObjectNode form, String id, ObjectNode expected) throws Exception
	{
		assertJson(exchange("PUT", PROJECT + "/registrationForm?ignoreRequests=false", form.toString(),
				ContentType.APPLICATION_JSON), 400, """
				{"message":"There are requests bound to this form, and it was not chosen to ignore them.",
				 "error":"SchemaConsistencyException"}
				""");
		assertJson(exchange("GET", REQUESTS + "/" + id), 200, expected);
		assertEmpty(exchange("PUT", PROJECT + "/registrationForm?ignoreRequests=true", form.toString(),
				ContentType.APPLICATION_JSON), 204);
		assertJson(exchange("GET", REQUESTS + "/" + id), 200, expected);
	}

	private void assertDeleteFlagsAndRestoreDetachedForm(String id, ObjectNode expected) throws Exception
	{
		assertJson(exchange("DELETE", PROJECT + "/registrationForm?dropRequests=false"), 400, """
				{"message":"There are requests bound to this form, and it was not chosen to drop them.",
				 "error":"SchemaConsistencyException"}
				""");
		assertThat(registrationsMan.getRegistrationRequest(id).getRequestId()).isEqualTo(id);
		assertThat(exchange("GET", PROJECT + "/registrationForm").status()).isEqualTo(404);
		assertJson(exchange("GET", PROJECT), 200, fixture("project.json"));
		var group = groupsMan.getContents("/A/contract", GroupContents.METADATA).getGroup();
		group.setDelegationConfiguration(GroupDelegationConfiguration.builder()
				.copy(group.getDelegationConfiguration()).withRegistrationForm("contract-form").build());
		groupsMan.updateGroup("/A/contract", group);
		assertJson(exchange("GET", REQUESTS + "/" + id), 200, expected);
		assertEmpty(exchange("DELETE", PROJECT + "/registrationForm?dropRequests=true"), 204);
	}
}
