/**
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.invite.FormProvider;
import pl.edu.icm.unity.base.registration.invite.InvitationParam;
import pl.edu.icm.unity.base.registration.invite.InvitationSendData;
import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.registration.UnknownInvitationException;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.tx.Transactional;

@Component
@Primary
@InvocationEventProducer
@Transactional
public class InvitationManagementImpl implements InvitationManagement
{
	private final InternalAuthorizationManager authz;
	private final InvitationDB invitationDB;
	private final FormProvider formProvider;
	private final InvitationSender sender;
	private final MessageSource msg;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;

	@Autowired
	public InvitationManagementImpl(InternalAuthorizationManager authz, InvitationDB invitationDB,
			InvitationSender sender, FormProvider formProvider, MessageSource msg,
			PublicRegistrationURLSupport publicRegistrationURLSupport)
	{
		this.authz = authz;
		this.invitationDB = invitationDB;
		this.sender = sender;
		this.formProvider = formProvider;
		this.msg = msg;
		this.publicRegistrationURLSupport = publicRegistrationURLSupport;
	}

	@Override
	public String addInvitation(InvitationParam invitation) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		invitation.validate(formProvider);

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
		InvitationSendData sendData = invitation.getSendData();
		BaseForm form = formProvider.getForm(sendData.form, sendData.formType);
		sender.sendInvitation(new ResolvedInvitationSendData(invitation.getSendData(),
				form.getNotificationsConfiguration().getInvitationTemplate(), form.getDisplayedName().getValue(msg),
				code, publicRegistrationURLSupport.getPublicFormLink(form.getName(), sendData.formType, code)));
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
		if (!invitationDB.exists(code))
			throw new UnknownInvitationException("Invitation with code " + code + " is unkwnown");
		return invitationDB.get(code);
	}

	@Override
	public void updateInvitation(String code, InvitationParam invitation) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		InvitationWithCode currentWithCode = invitationDB.get(code);
		InvitationParam current = currentWithCode.getInvitation();
		if (!Objects.equal(current.getContactAddress(), invitation.getContactAddress()))
			throw new WrongArgumentException("Can not update contact address of an invitation");
		current.validateUpdate(invitation);
		InvitationWithCode updated = new InvitationWithCode(invitation, currentWithCode.getRegistrationCode(),
				currentWithCode.getLastSentTime(), currentWithCode.getNumberOfSends());
		invitationDB.update(updated);
	}
}
