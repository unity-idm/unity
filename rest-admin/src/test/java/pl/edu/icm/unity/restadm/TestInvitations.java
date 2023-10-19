/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response.Status;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestIdentityParam;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import io.imunity.rest.api.types.registration.RestGroupSelection;
import io.imunity.rest.api.types.registration.invite.RestFormPrefill;
import io.imunity.rest.api.types.registration.invite.RestInvitationParam;
import io.imunity.rest.api.types.registration.invite.RestInvitationWithCode;
import io.imunity.rest.api.types.registration.invite.RestPrefilledEntry;
import io.imunity.rest.api.types.registration.invite.RestRegistrationInvitationParam;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.engine.builders.NotificationChannelBuilder;
import pl.edu.icm.unity.engine.notifications.email.EmailFacility;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * Invitations management test
 * 
 * @author Krzysztof Benedyczak
 */
public class TestInvitations extends RESTAdminTestBase
{
	@BeforeEach
	public void addForm() throws EngineException
	{
		aTypeMan.addAttributeType(new AttributeType("cn", StringAttributeSyntax.ID));
		registrationsMan.addForm(getRegistrationForm());
	}

	@Test
	public void addedInvitationIsReturned() throws Exception
	{
		RestInvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);
		HttpGet get = new HttpGet("/restadm/v1/invitation/" + code);
		String contentsGet = executeQuery(get);
		System.out.println("Response:\n" + contentsGet);
		RestInvitationWithCode returned = m.readValue(contentsGet, RestInvitationWithCode.class);
		assertThat(returned.registrationCode).isEqualTo(code);
		assertThat(returned.invitation).isEqualTo(invitation);
	}

	@Test
	public void addedInvitationIsReturnedInList() throws Exception
	{
		RestInvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);

		HttpGet get = new HttpGet("/restadm/v1/invitations");
		String contentsGet = executeQuery(get);
		System.out.println("Response:\n" + contentsGet);
		List<RestInvitationWithCode> returned = m.readValue(contentsGet, new TypeReference<List<RestInvitationWithCode>>()
		{
		});
		assertThat(returned.size()).isEqualTo(1);
		assertThat(returned.get(0)
				.registrationCode).isEqualTo(code);
		assertThat(returned.get(0)
				.invitation).isEqualTo(invitation);
	}

	@Test
	public void removedInvitationIsNotReturned() throws Exception
	{
		RestInvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);

		HttpDelete delete = new HttpDelete("/restadm/v1/invitation/" + code);
		try (ClassicHttpResponse response = client.executeOpen(host, delete, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet get = new HttpGet("/restadm/v1/invitations");
		String contentsGet = executeQuery(get);
		List<RestInvitationWithCode> returned = m.readValue(contentsGet, new TypeReference<List<RestInvitationWithCode>>()
		{
		});
		assertThat(returned.isEmpty()).isEqualTo(true);
	}

	@Test
	public void triggeringInvitationSendWorks() throws Exception
	{
		notMan.addNotificationChannel(NotificationChannelBuilder.notificationChannel()
				.withName("channelId")
				.withDescription("")
				.withConfiguration("")
				.withFacilityId(EmailFacility.NAME)
				.build());

		RestInvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);

		HttpPost send = new HttpPost("/restadm/v1/invitation/" + code + "/send");
		try (ClassicHttpResponse response = client.executeOpen(host, send, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet get = new HttpGet("/restadm/v1/invitation/" + code);
		String contentsGet = executeQuery(get);
		System.out.println("Response:\n" + contentsGet);
		RestInvitationWithCode returned = m.readValue(contentsGet, RestInvitationWithCode.class);

		assertThat(returned.numberOfSends).isEqualTo(1);
		assertThat(returned.lastSentTime).isNotNull();
	}

	private String addInvitation(RestInvitationParam invitation) throws Exception
	{
		HttpPost addRequest = getAddRequest(invitation);
		ClassicHttpResponse responseAdd = client.executeOpen(host, addRequest, getClientContext(host));
		assertEquals(Status.OK.getStatusCode(), responseAdd.getCode());
		return EntityUtils.toString(responseAdd.getEntity());
	}

	private void configureRequest(HttpUriRequestBase request, RestInvitationParam invitation)
			throws UnsupportedEncodingException, JsonProcessingException
	{
		String jsonform = m.writeValueAsString(invitation);
		System.out.println("Request to be sent:\n" + jsonform);
		request.setEntity(new StringEntity(jsonform, ContentType.APPLICATION_JSON));
	}

	private HttpPost getAddRequest(RestInvitationParam invitation) throws Exception
	{
		HttpPost addForm = new HttpPost("/restadm/v1/invitation");
		configureRequest(addForm, invitation);
		return addForm;
	}

	private RestInvitationParam createInvitation()
	{
		return RestRegistrationInvitationParam.builder()
				.withContactAddress("someAddr@example.com")
				.withExpiration(Instant.now()
						.plusSeconds(200)
						.truncatedTo(ChronoUnit.SECONDS)
						.toEpochMilli())
				.withFormPrefill(RestFormPrefill.builder()
						.withFormId("exForm")
						.withAttributes(Map.of(0,
								new RestPrefilledEntry.Builder<RestAttribute>().withEntry(RestAttribute.builder()
										.withGroupPath("/")
										.withName("cn")
										.withValues(List.of("value"))
										.withValueSyntax("string")
										.build())
										.withMode("READ_ONLY")
										.build()))
						.withIdentities(Map.of(0,
								new RestPrefilledEntry.Builder<RestIdentityParam>().withMode("READ_ONLY")
										.withEntry(RestIdentityParam.builder()
												.withTypeId("userName")
												.withValue("user-id")
												.withConfirmationInfo(RestConfirmationInfo.builder()
														.build())
												.build())
										.build()))
						.withGroupSelections(Map.of(0,
								new RestPrefilledEntry.Builder<RestGroupSelection>().withMode("READ_ONLY")
										.withEntry(RestGroupSelection.builder()
												.withSelectedGroups(List.of("/"))
												.build())
										.build()))
						.build())
				.build();
	}

	private RegistrationForm getRegistrationForm()
	{
		IdentityRegistrationParam idParam = new IdentityRegistrationParam();
		idParam.setIdentityType(UsernameIdentity.ID);
		idParam.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		return new RegistrationFormBuilder().withName("exForm")
				.withAddedIdentityParam(idParam)
				.withAddedGroupParam()
				.withGroupPath("/")
				.endGroupParam()
				.withAddedAttributeParam()
				.withAttributeType("cn")
				.withGroup("/")
				.endAttributeParam()
				.withNotificationsConfiguration()
				.withInvitationTemplate("invitationWithCode")
				.endNotificationsConfiguration()
				.withPubliclyAvailable(true)
				.withCollectComments(true)
				.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.build();
	}
}
