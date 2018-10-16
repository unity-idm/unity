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
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

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
	private RegistrationFormDB formsDB;
	private AuthorizationManager authz;
	private NotificationProducer notificationProducer;

	private UnityMessageSource msg;
	private InvitationDB invitationDB;
	private SharedEndpointManagement sharedEndpointMan;

	@Autowired
	public InvitationManagementImpl(RegistrationFormDB formsDB, AuthorizationManager authz,
			NotificationProducer notificationProducer, UnityMessageSource msg,
			InvitationDB invitationDB, SharedEndpointManagement sharedEndpointMan)
	{
		this.formsDB = formsDB;
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
		RegistrationForm form = formsDB.get(invitation.getFormId());
		if (!form.isPubliclyAvailable())
			throw new WrongArgumentException("Invitations can be attached to public forms only");
		if (form.getRegistrationCode() != null)
			throw new WrongArgumentException("Invitations can not be attached to forms with a fixed registration code");
		String randomUUID = UUID.randomUUID().toString();
		InvitationWithCode withCode = new InvitationWithCode(invitation, randomUUID, null, 0);
		invitationDB.create(withCode);
		return randomUUID;
	}

	@Override
	public void sendInvitation(String code) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		String userLocale = msg.getDefaultLocaleCode();
		
		InvitationWithCode invitation = invitationDB.get(code);
		if (invitation.getContactAddress() == null)
			throw new WrongArgumentException("The invitation has no contact address configured");
		if (invitation.getExpiration().isBefore(Instant.now()))
			throw new WrongArgumentException("The invitation is expired");
		
		RegistrationForm form = formsDB.get(invitation.getFormId());
		if (form.getNotificationsConfiguration().getInvitationTemplate() == null)
			throw new WrongArgumentException("The form of the invitation has no invitation message "
					+ "template configured");
		
		Map<String, String> notifyParams = new HashMap<>();
		notifyParams.put(BaseRegistrationTemplateDef.FORM_NAME, form.getDisplayedName().getValue(
				userLocale, msg.getDefaultLocaleCode()));
		notifyParams.put(InvitationTemplateDef.CODE, invitation.getRegistrationCode());
		notifyParams.put(InvitationTemplateDef.URL, 
				PublicRegistrationURLSupport.getPublicRegistrationLink(form, code, 
						sharedEndpointMan));
		ZonedDateTime expiry = invitation.getExpiration().atZone(ZoneId.systemDefault());
		notifyParams.put(InvitationTemplateDef.EXPIRES, expiry.format(DateTimeFormatter.RFC_1123_DATE_TIME));
		notifyParams.putAll(invitation.getMessageParams());
		
		Instant sentTime = Instant.now();
		notificationProducer.sendNotification(invitation.getContactAddress(),
				form.getNotificationsConfiguration().getInvitationTemplate(),
				notifyParams, userLocale);
		
		invitation.setLastSentTime(sentTime);
		invitation.setNumberOfSends(invitation.getNumberOfSends() + 1);
		invitationDB.update(invitation);
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
		InvitationWithCode current = invitationDB.get(code);
		if (!Objects.equal(current.getFormId(), invitation.getFormId()))
			throw new WrongArgumentException("Can not update form of an invitation");
		if (!Objects.equal(current.getContactAddress(), invitation.getContactAddress()))
			throw new WrongArgumentException("Can not update contact address of an invitation");
		InvitationWithCode updated = new InvitationWithCode(invitation, current.getRegistrationCode(), 
				current.getLastSentTime(), current.getNumberOfSends());
		invitationDB.update(updated);
	}
}
