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

import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

@Component
@Primary
@InvocationEventProducer
@Transactional
public class InvitationManagementImpl implements InvitationManagement
{	
	private final InternalAuthorizationManager authz;
	private final InvitationDB invitationDB;
	private final InvitationValidator validator;

	private final InvitationSender sender;

	@Autowired
	public InvitationManagementImpl(InternalAuthorizationManager authz, InvitationDB invitationDB,
			InvitationValidator validator, InvitationSender sender)
	{
		this.authz = authz;
		this.invitationDB = invitationDB;
		this.validator = validator;
		this.sender = sender;
	}

	@Override
	public String addInvitation(InvitationParam invitation) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateInvitation(invitation);

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
		invitation.send(sender, code);
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

		if (!Objects.equal(current.getType(), invitation.getType()))
			throw new WrongArgumentException("Can not update type of invitation");
		if (!Objects.equal(current.getContactAddress(), invitation.getContactAddress()))
			throw new WrongArgumentException("Can not update contact address of an invitation");
		current.validateUpdate(validator, invitation);
		InvitationWithCode updated = new InvitationWithCode(invitation, currentWithCode.getRegistrationCode(),
				currentWithCode.getLastSentTime(), currentWithCode.getNumberOfSends());
		invitationDB.update(updated);
	}

	private void validateInvitation(InvitationParam invitation) throws EngineException
	{
		invitation.validate(validator);
	}
}
