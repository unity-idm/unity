package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;

class AdminWorkflowHttpContractTest extends AdminTransportHttpTestBase
{
	@Test
	void shouldPreserveFlattenedInvitationWithMillisecondTimestamps() throws Exception
	{
		var parameters = new RegistrationInvitationParam("form", Instant.parse("2035-01-01T00:00:00.125Z"),
				"żółć@example.com");
		var invitation = new InvitationWithCode(parameters, "code", Instant.ofEpochSecond(123, 250_000_000), 2);
		invitation.setCreationTime(Instant.ofEpochSecond(100));
		when(invitationMan.getInvitation("code")).thenReturn(invitation);
		when(invitationMan.getInvitations()).thenReturn(List.of(invitation));
		String expected = fixture("invitation.json");

		assertJson("/v1/invitation/code", expected);
		assertJson("/v1/invitations", "[" + expected + "]");
		when(invitationMan.addInvitation(any())).thenReturn("literal-code");
		var created = exchange("POST", "/v1/invitation", fixture("invitation-create.json"), "application/json");
		assertThat(created.statusCode()).as(created.body()).isEqualTo(200);
		assertThat(created.headers().firstValue("content-type").orElse("")).startsWith("text/plain");
		assertThat(created.body()).isEqualTo("literal-code");
	}

	@Test
	void shouldPreservePopulatedRequestAndResponseTrees() throws Exception
	{
		var registration = new RegistrationRequestState();
		var input = new RegistrationRequest();
		input.setFormId("form");
		input.setComments("Zażółć ☃");
		registration.setRequest(input);
		registration.setRequestId("request");
		registration.setTimestamp(new Date(1234));
		registration.setStatus(RegistrationRequestStatus.pending);
		registration.setRegistrationContext(new RegistrationContext(false,
				RegistrationContext.TriggeringMode.manualAtLogin));
		var enquiry = new EnquiryResponseState();
		var response = new EnquiryResponse();
		response.setFormId("form");
		response.setComments("Zażółć ☃");
		enquiry.setRequest(response);
		enquiry.setRequestId("request");
		enquiry.setTimestamp(new Date(1234));
		enquiry.setStatus(RegistrationRequestStatus.pending);
		enquiry.setRegistrationContext(registration.getRegistrationContext());
		enquiry.setEntityId(42);
		when(registrationManagement.getRegistrationRequests()).thenReturn(List.of(registration));
		when(enquiryManagement.getEnquiryResponses()).thenReturn(List.of(enquiry));
		String common = """
				"RequestId":"request","Timestamp":1234,"Status":"pending","AdminComments":[],
				"Context":{"isOnIdpEndpoint":false,"triggeringMode":"manualAtLogin"},
				"FormId":"form","Identities":[],"Attributes":[],"Credentials":[],"GroupSelections":[],
				"Agreements":[],"PolicyAgreements":[],"Comments":"Zażółć ☃","UserLocale":null,
				"RegistrationCode":null
				""";

		assertJson("/v1/registrationRequest/request", fixture("registration-request.json"));
		assertJson("/v1/registrationRequests", "[{" + common + "}]");
		assertJson("/v1/enquiryResponse/request", "{" + common + ",\"EntityId\":42}");
		assertJson("/v1/enquiryResponses", "[{" + common + ",\"EntityId\":42}]");
	}

	@Test
	void shouldPreserveEmptyQueriesAndBulkProcessingPlainResponses() throws Exception
	{
		assertJson("/v1/endpoints", "[]");
		assertJson("/v1/tokens", "[]");
		assertJson("/v1/idp-stats", "[]");
		var imported = exchange("POST", "/v1/import/user/identity", null, null);
		assertThat(imported.statusCode()).as(imported.body()).isEqualTo(200);
		assertThat(json.readTree(imported.body())).isEqualTo(json.readTree("[]"));
		for (String timeout : new String[] {"-1", "1000"})
		{
			var result = exchange("POST", "/v1/bulkProcessing/instant?timeout=" + timeout,
					"{\"condition\":\"true\",\"action\":{\"name\":\"noop\",\"parameters\":[]}}", "application/json");
			assertThat(result.statusCode()).as(result.body()).isEqualTo(200);
			assertThat(result.body()).isEqualTo(timeout.equals("-1") ? "async" : "sync");
		}
	}

	@Test
	void shouldPreserveFormattedTokenContentsAndNullableMetadata() throws Exception
	{
		var token = new pl.edu.icm.unity.base.token.Token("type", "value", 42L);
		token.setCreated(new Date(123));
		token.setContents(new byte[] {1});
		when(securedTokenMan.getAllTokens(any())).thenReturn(List.of(token));
		String expected = fixture("token.json");
		when(jsonFormatter.toJson(any())).thenReturn((com.fasterxml.jackson.databind.node.ObjectNode)
				json.readTree(expected));

		assertJson("/v1/tokens", "[" + expected + "]");
	}
}
