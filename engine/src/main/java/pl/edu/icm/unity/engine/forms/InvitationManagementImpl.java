/**
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;

import pl.edu.icm.unity.base.msgtemplates.reg.BaseRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationTemplateDef;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

/**
 * Implementation of {@link InvitationManagement}
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
@Transactional
public class InvitationManagementImpl implements InvitationManagement
{
	private RegistrationFormDB registrationDB;
	private EnquiryFormDB enquiryDB;
	private InternalAuthorizationManager authz;
	private NotificationProducer notificationProducer;

	private UnityMessageSource msg;
	private InvitationDB invitationDB;
	private SharedEndpointManagement sharedEndpointMan;

	@Autowired
	public InvitationManagementImpl(RegistrationFormDB registrationDB, EnquiryFormDB  enquiryDB,
			InternalAuthorizationManager authz,
			NotificationProducer notificationProducer, UnityMessageSource msg,
			InvitationDB invitationDB, SharedEndpointManagement sharedEndpointMan)
	{
		this.registrationDB = registrationDB;
		this.enquiryDB = enquiryDB;
		this.authz = authz;
		this.notificationProducer = notificationProducer;
		this.msg = msg;
		this.invitationDB = invitationDB;
		this.sharedEndpointMan = sharedEndpointMan;
	}

	@Override
	public String addInvitation(InvitationParam invitation) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
	
		if (invitation.getFormId() == null)
		{
			throw new WrongArgumentException("The invitation has no form configured");
		}
		
		
		if (invitation.getType().equals(InvitationType.REGISTRATION))
		{
			assertRegistrationFormIsForInvitation(invitation.getFormId());
		}else
		{
			assertEnquiryFormIsPresent(invitation.getFormId());
		}
		
		String randomUUID = UUID.randomUUID().toString();
		InvitationWithCode withCode = new InvitationWithCode(invitation, randomUUID, null, 0);
		invitationDB.create(withCode);
		return randomUUID;
	}
	
	@Override
	public void sendInvitation(String code) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		InvitationWithCode invitationWithCode = invitationDB.get(code);
		InvitationParam invitation = invitationWithCode.getInvitation();
		if (invitation.getExpiration().isBefore(Instant.now()))
			throw new WrongArgumentException("The invitation is expired");

		if (invitation.getType().equals(InvitationType.REGISTRATION))
		{
			sendRegistrationInvitation((RegistrationInvitationParam) invitation, code);

		} else
		{
			sendEnquiryInvitation((EnquiryInvitationParam) invitation, code);
		}

		Instant sentTime = Instant.now();
		invitationWithCode.setLastSentTime(sentTime);
		invitationWithCode.setNumberOfSends(invitationWithCode.getNumberOfSends() + 1);
		invitationDB.update(invitationWithCode);
	}
	
	@Override
	public void removeInvitation(String code) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		invitationDB.delete(code);
	}

	@Override
	public List<InvitationWithCode> getInvitations() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return invitationDB.getAll();
	}

	@Override
	public InvitationWithCode getInvitation(String code) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return invitationDB.get(code);
	}

	@Override
	public void updateInvitation(String code, InvitationParam invitation) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		InvitationWithCode currentWithCode = invitationDB.get(code);
		InvitationParam current = currentWithCode.getInvitation();
		
		if (!Objects.equal(current.getFormId(), invitation.getFormId()))
			throw new WrongArgumentException("Can not update form of an invitation");
		if (!Objects.equal(current.getContactAddress(), invitation.getContactAddress()))
			throw new WrongArgumentException("Can not update contact address of an invitation");
		InvitationWithCode updated = new InvitationWithCode(invitation, currentWithCode.getRegistrationCode(), 
				currentWithCode.getLastSentTime(), currentWithCode.getNumberOfSends());
		invitationDB.update(updated);
	}
	
	private void assertEnquiryFormIsPresent(String formId)
	{
		enquiryDB.get(formId);		
	}

	private void assertRegistrationFormIsForInvitation(String formId) throws WrongArgumentException
	{
		RegistrationForm form = registrationDB.get(formId);
		if (!form.isPubliclyAvailable())
			throw new WrongArgumentException("Invitations can be attached to public forms only");
		if (form.getRegistrationCode() != null)
			throw new WrongArgumentException("Invitations can not be attached to forms with a fixed registration code");
	}
	
	private void sendRegistrationInvitation(RegistrationInvitationParam invitation, String code)
			throws EngineException
	{	
		RegistrationForm form = registrationDB.get(invitation.getFormId());
		sendInvitation(form, invitation.getExpiration(), invitation.getMessageParams(),
				invitation.getContactAddress(),
				PublicRegistrationURLSupport.getPublicRegistrationLink(form, code, sharedEndpointMan),
				code);
	}

	private void sendEnquiryInvitation(EnquiryInvitationParam invitation, String code) throws EngineException
	{
		if (invitation.getEntity() == null)
			throw new WrongArgumentException("The invitation has no entity configured");
		
		EnquiryForm form = enquiryDB.get(invitation.getFormId());
		sendInvitation(form, invitation.getExpiration(), invitation.getMessageParams(), invitation.getContactAddress(),
				PublicRegistrationURLSupport.getPublicEnquiryLink(form, code, sharedEndpointMan), code);
	}

	private void sendInvitation(BaseForm form, Instant expiration, Map<String, String> messageParams,
			String contactAdress, String url, String code) throws EngineException
	{
		if (contactAdress == null)
			throw new WrongArgumentException("The invitation has no contact address configured");
		if (form.getNotificationsConfiguration().getInvitationTemplate() == null)
			throw new WrongArgumentException("The form of the invitation has no invitation message "
					+ "template configured");
		
		String userLocale = msg.getDefaultLocaleCode();
		Map<String, String> notifyParams = new HashMap<>();
		notifyParams.put(BaseRegistrationTemplateDef.FORM_NAME,
				form.getDisplayedName().getValue(userLocale, msg.getDefaultLocaleCode()));
		notifyParams.put(InvitationTemplateDef.CODE, code);
		notifyParams.put(InvitationTemplateDef.URL, url);
		ZonedDateTime expiry = expiration.atZone(ZoneId.systemDefault());
		notifyParams.put(InvitationTemplateDef.EXPIRES, expiry.format(DateTimeFormatter.RFC_1123_DATE_TIME));
		notifyParams.putAll(messageParams);

		notificationProducer.sendNotification(contactAdress,
				form.getNotificationsConfiguration().getInvitationTemplate(), notifyParams, userLocale);
	}
	
}
