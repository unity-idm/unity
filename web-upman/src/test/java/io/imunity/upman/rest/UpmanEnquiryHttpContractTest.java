package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.registration.EnquiryResponseBuilder;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.engine.api.EnquiryManagement;

class UpmanEnquiryHttpContractTest extends UpmanHttpContractTestBase
{
	private static final String PROJECTS = "/restupm/v1/projects";
	private static final String PROJECT = PROJECTS + "/contract";
	private static final String REQUESTS = PROJECT + "/requests";

	@Autowired
	private EnquiryManagement enquiries;

	@Test
	void shouldPreserveSignupEnquiryRequestsAndPendingResponseFlags() throws Exception
	{
		assertEnquiryContract("signUpEnquiry", "enquiry-form-create.json", "SignUp");
	}

	@Test
	void shouldPreserveMembershipEnquiryRequestsAndPendingResponseFlags() throws Exception
	{
		assertEnquiryContract("membershipUpdateEnquiry", "membership-form-create.json", "Update");
	}

	private void assertEnquiryContract(String route, String fixture, String operation) throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		String formPath = PROJECT + "/" + route;
		String form = fixture(fixture);
		assertEmpty(exchange("POST", formPath, form, ContentType.APPLICATION_JSON), 204);
		for (String decision : new String[] {"accept", "decline", "drop"})
		{
			var response = new EnquiryResponseBuilder().withFormId("contract-form")
					.withAddedGroupSelection().withGroup("/A/contract").endGroupSelection()
					.withAddedAgreement().withSelected(false).endAgreement().build();
			setupUserContext(DEF_USER, null);
			String id;
			try
			{
				id = enquiries.submitEnquiryResponse(response,
						new RegistrationContext(false, TriggeringMode.manualStandalone));
			} finally
			{
				setupAdmin();
			}
			ObjectNode expected = (ObjectNode) m.readTree(fixture("request.json"));
			expected.put("id", id).put("operation", operation).put("type", "Enquiry").put("email", entityEmail);
			expected.put("requestedTime", BigDecimal.valueOf(enquiries.getEnquiryResponse(id).getTimestamp().getTime(), 3));
			assertJson(exchange("GET", REQUESTS + "/" + id), 200, expected);
			assertJson(exchange("GET", REQUESTS), 200, m.createArrayNode().add(expected));
			assertJson(exchange("PUT", formPath + "?ignoreRequests=false", form, ContentType.APPLICATION_JSON), 400,
					"""
					{"message":"There are requests bound to this form, and it was not chosen to ignore them.",
					 "error":"SchemaConsistencyException"}
					""");
			assertJson(exchange("GET", REQUESTS + "/" + id), 200, expected);
			assertEmpty(exchange("PUT", formPath + "?ignoreRequests=true", form, ContentType.APPLICATION_JSON), 204);
			assertJson(exchange("GET", REQUESTS + "/" + id), 200, expected);
			if (decision.equals("drop"))
			{
				assertJson(exchange("DELETE", formPath + "?dropRequests=false"), 400, """
						{"message":"There are requests bound to this form, and it was not chosen to drop them.",
						 "error":"SchemaConsistencyException"}
						""");
				assertThat(enquiries.getEnquiryResponse(id).getRequestId()).isEqualTo(id);
				assertEmpty(exchange("GET", formPath), 404);
				var group = groupsMan.getContents("/A/contract", GroupContents.METADATA).getGroup();
				var config = GroupDelegationConfiguration.builder().copy(group.getDelegationConfiguration());
				if (route.equals("signUpEnquiry"))
					config.withSignupEnquiryForm("contract-form");
				else
					config.withMembershipUpdateEnquiryForm("contract-form");
				group.setDelegationConfiguration(config.build());
				groupsMan.updateGroup("/A/contract", group);
				assertEmpty(exchange("DELETE", formPath + "?dropRequests=true"), 204);
			} else
				assertEmpty(exchange("POST", REQUESTS + "/" + id + "/state?decision=" + decision), 204);
			assertJson(exchange("GET", REQUESTS), 200, "[]");
			assertEmpty(exchange("GET", REQUESTS + "/" + id), 404);
		}
	}
}
