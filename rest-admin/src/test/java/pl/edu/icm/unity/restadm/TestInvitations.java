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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import pl.edu.icm.unity.engine.builders.NotificationChannelBuilder;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.engine.notifications.EmailFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeParamRepresentation;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.invite.RESTInvitationParam;
import pl.edu.icm.unity.types.registration.invite.RESTInvitationWithCode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Invitations management test
 * @author Krzysztof Benedyczak
 */
public class TestInvitations extends RESTAdminTestBase
{
	@Before
	public void addForm() throws EngineException
	{
		attrsMan.addAttributeType(new AttributeType("cn", new StringAttributeSyntax()));
		registrationsMan.addForm(getRegistrationForm());
	}
	
	@Test
	public void addedInvitationIsReturned() throws Exception
	{
		RESTInvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);

		HttpGet get = new HttpGet("/restadm/v1/invitation/"+code);
		HttpResponse responseGet = client.execute(host, get, localcontext);

		String contentsGet = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contentsGet);
		assertEquals(contentsGet, Status.OK.getStatusCode(), responseGet.getStatusLine().getStatusCode());
		RESTInvitationWithCode returned = m.readValue(contentsGet, RESTInvitationWithCode.class);
		assertThat(returned.getRegistrationCode(), is(code));
		RESTInvitationWithCode source = new RESTInvitationWithCode(invitation, code, null, 0);
		assertThat(returned, is(source));
	}
	
	@Test
	public void addedInvitationIsReturnedInList() throws Exception
	{
		RESTInvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);

		HttpGet get = new HttpGet("/restadm/v1/invitations");
		HttpResponse responseGet = client.execute(host, get, localcontext);

		String contentsGet = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contentsGet);
		assertEquals(contentsGet, Status.OK.getStatusCode(), responseGet.getStatusLine().getStatusCode());
		
		
		List<RESTInvitationWithCode> returned = m.readValue(contentsGet, 
				new TypeReference<List<RESTInvitationWithCode>>() {});
		assertThat(returned.size(), is(1));
		assertThat(returned.get(0).getRegistrationCode(), is(code));
		RESTInvitationWithCode source = new RESTInvitationWithCode(invitation, code, null, 0);
		assertThat(returned.get(0), is(source));
	}
	
	@Test
	public void removedInvitationIsNotReturned() throws Exception
	{
		RESTInvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);
		
		HttpDelete delete = new HttpDelete("/restadm/v1/invitation/" + code);
		HttpResponse deleteResponse = client.execute(host, delete, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatusLine().getStatusCode());
		
		HttpGet get = new HttpGet("/restadm/v1/invitations");
		HttpResponse responseGet = client.execute(host, get, localcontext);
		String contentsGet = EntityUtils.toString(responseGet.getEntity());
		assertEquals(contentsGet, Status.OK.getStatusCode(), responseGet.getStatusLine().getStatusCode());
		List<RESTInvitationWithCode> returned = m.readValue(contentsGet, 
				new TypeReference<List<RESTInvitationWithCode>>() {});
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
		
		RESTInvitationParam invitation = createInvitation();
		String code = addInvitation(invitation);
		
		HttpPost send = new HttpPost("/restadm/v1/invitation/" + code + "/send");
		HttpResponse responseSend = client.execute(host, send, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), responseSend.getStatusLine().getStatusCode());
		
		HttpGet get = new HttpGet("/restadm/v1/invitation/"+code);
		HttpResponse responseGet = client.execute(host, get, localcontext);

		String contentsGet = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contentsGet);
		assertEquals(contentsGet, Status.OK.getStatusCode(), responseGet.getStatusLine().getStatusCode());
		RESTInvitationWithCode returned = m.readValue(contentsGet, RESTInvitationWithCode.class);

		assertThat(returned.getNumberOfSends(), is(1));
		assertThat(returned.getLastSentTime(), is(notNullValue()));
	}
	
	private String addInvitation(RESTInvitationParam invitation) throws Exception
	{
		HttpPost addRequest = getAddRequest(invitation);
		HttpResponse responseAdd = client.execute(host, addRequest, localcontext);
		assertEquals(Status.OK.getStatusCode(), responseAdd.getStatusLine().getStatusCode());
		return EntityUtils.toString(responseAdd.getEntity());
	}
	
	private void configureRequest(HttpEntityEnclosingRequestBase request, RESTInvitationParam invitation)
			throws UnsupportedEncodingException, JsonProcessingException
	{
		String jsonform = m.writeValueAsString(invitation);
		System.out.println("Request to be sent:\n" + jsonform);
		request.setEntity(new StringEntity(jsonform, ContentType.APPLICATION_JSON));
	}
	
	private HttpPost getAddRequest(RESTInvitationParam invitation) throws Exception
	{
		HttpPost addForm = new HttpPost("/restadm/v1/invitation");
		configureRequest(addForm, invitation);
		return addForm;
	}
	
	private RESTInvitationParam createInvitation()
	{
		RESTInvitationParam ret = new RESTInvitationParam("exForm", 
				Instant.now().plusSeconds(200).truncatedTo(ChronoUnit.SECONDS), 
				"someAddr@example.com", "channelId");
		AttributeParamRepresentation attrP = new AttributeParamRepresentation(
				new StringAttribute("cn", "/", AttributeVisibility.full, "value"));
		ret.getAttributes().put(0, new PrefilledEntry<>(attrP, PrefilledEntryMode.READ_ONLY));
		ret.getIdentities().put(0, new PrefilledEntry<>(new IdentityParam(UsernameIdentity.ID, 
				"user-id"), PrefilledEntryMode.READ_ONLY));
		ret.getGroupSelections().put(0, new PrefilledEntry<>(new Selection(true), 
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
