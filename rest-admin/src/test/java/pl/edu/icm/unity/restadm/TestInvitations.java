/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import pl.edu.icm.unity.engine.builders.NotificationChannelBuilder;
import pl.edu.icm.unity.engine.notifications.email.EmailFacility;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

/**
 * Invitations management test
 * @author Krzysztof Benedyczak
 */
public class TestInvitations extends RESTAdminTestBase
{
	@Before
	public void addForm() throws EngineException
	{
		aTypeMan.addAttributeType(new AttributeType("cn", StringAttributeSyntax.ID));
		registrationsMan.addForm(getRegistrationForm());
	}
	
	@Test
	public void addedInvitationIsReturned() throws Exception
	{
		InvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);
		HttpGet get = new HttpGet("/restadm/v1/invitation/"+code);
		String contentsGet = executeQuery(get);
		System.out.println("Response:\n" + contentsGet);
		InvitationWithCode returned = m.readValue(contentsGet, InvitationWithCode.class);
		assertThat(returned.getRegistrationCode(), is(code));
		assertThat(returned.getInvitation(), is(invitation));
	}
	
	@Test
	public void addedInvitationIsReturnedInList() throws Exception
	{
		InvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);

		HttpGet get = new HttpGet("/restadm/v1/invitations");
		String contentsGet = executeQuery(get);
		System.out.println("Response:\n" + contentsGet);
		List<InvitationWithCode> returned = m.readValue(contentsGet, 
				new TypeReference<List<InvitationWithCode>>() {});
		assertThat(returned.size(), is(1));
		assertThat(returned.get(0).getRegistrationCode(), is(code));
	
		assertThat(returned.get(0).getRegistrationCode(), is(code));
		assertThat(returned.get(0).getInvitation(), is(invitation));
	}
	
	@Test
	public void removedInvitationIsNotReturned() throws Exception
	{
		InvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);
		
		HttpDelete delete = new HttpDelete("/restadm/v1/invitation/" + code);
		try(ClassicHttpResponse response = client.executeOpen(host, delete, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		
		HttpGet get = new HttpGet("/restadm/v1/invitations");
		String contentsGet = executeQuery(get);
		List<InvitationWithCode> returned = m.readValue(contentsGet, 
				new TypeReference<List<InvitationWithCode>>() {});
		assertThat(returned.isEmpty(), is(true));
	}
	
	@Test
	public void triggeringInvitationSendWorks() throws Exception
	{
		notMan.addNotificationChannel(NotificationChannelBuilder.notificationChannel()
				.withName("channelId")
				.withDescription("")
				.withConfiguration("")
				.withFacilityId(EmailFacility.NAME).build());
		
		InvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);
		
		HttpPost send = new HttpPost("/restadm/v1/invitation/" + code + "/send");
		try(ClassicHttpResponse response = client.executeOpen(host, send, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		
		HttpGet get = new HttpGet("/restadm/v1/invitation/"+code);
		String contentsGet = executeQuery(get);
		System.out.println("Response:\n" + contentsGet);
		InvitationWithCode returned = m.readValue(contentsGet, InvitationWithCode.class);

		assertThat(returned.getNumberOfSends(), is(1));
		assertThat(returned.getLastSentTime(), is(notNullValue()));
	}
	
	private String addInvitation(InvitationParam invitation) throws Exception
	{
		HttpPost addRequest = getAddRequest(invitation);
		ClassicHttpResponse responseAdd = client.executeOpen(host, addRequest, getClientContext(host));
		assertEquals(Status.OK.getStatusCode(), responseAdd.getCode());
		return EntityUtils.toString(responseAdd.getEntity());
	}
	
	private void configureRequest(HttpUriRequestBase request, InvitationParam invitation)
			throws UnsupportedEncodingException, JsonProcessingException
	{
		String jsonform = m.writeValueAsString(invitation);
		System.out.println("Request to be sent:\n" + jsonform);
		request.setEntity(new StringEntity(jsonform, ContentType.APPLICATION_JSON));
	}
	
	private HttpPost getAddRequest(InvitationParam invitation) throws Exception
	{
		HttpPost addForm = new HttpPost("/restadm/v1/invitation");
		configureRequest(addForm, invitation);
		return addForm;
	}
	
	private InvitationParam createInvitation()
	{
		RegistrationInvitationParam ret = new RegistrationInvitationParam("exForm", 
				Instant.now().plusSeconds(200).truncatedTo(ChronoUnit.SECONDS), 
				"someAddr@example.com");
		Attribute attrP = StringAttribute.of("cn", "/", "value");
		ret.getFormPrefill().getAttributes().put(0, new PrefilledEntry<>(attrP, PrefilledEntryMode.READ_ONLY));
		ret.getFormPrefill().getIdentities().put(0, new PrefilledEntry<>(new IdentityParam(UsernameIdentity.ID, 
				"user-id"), PrefilledEntryMode.READ_ONLY));
		ret.getFormPrefill().getGroupSelections().put(0, new PrefilledEntry<>(new GroupSelection("/"), 
				PrefilledEntryMode.READ_ONLY));
		return ret;
	}
	
	private RegistrationForm getRegistrationForm()
	{
		IdentityRegistrationParam idParam = new IdentityRegistrationParam();
		idParam.setIdentityType(UsernameIdentity.ID);
		idParam.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		return new RegistrationFormBuilder()
			.withName("exForm")
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
