/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.identity.UnknownEmailException;
import pl.edu.icm.unity.engine.api.registration.UnknownInvitationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;

@Component
public class InvitationResolver
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InvitationResolver.class);

	private final InvitationManagement invitationMan;
	private final EntityManagement entityManagement;

	@Autowired
	public InvitationResolver(@Qualifier("insecure") InvitationManagement invitationMan,
			@Qualifier("insecure") EntityManagement entityManagement)
	{
		this.invitationMan = invitationMan;
		this.entityManagement = entityManagement;
	}

	public ResolvedInvitationParam getInvitationByCode(String registrationCode) throws RegCodeException
	{
		if (registrationCode == null)
			throw new RegCodeException(ErrorCause.MISSING_CODE);
		InvitationWithCode invitation = getInvitationInternal(registrationCode).orElse(null);
		if (invitation == null)
			throw new RegCodeException(ErrorCause.UNRESOLVED_INVITATION);
		if (invitation.getInvitation().isExpired())
			throw new RegCodeException(ErrorCause.EXPIRED_INVITATION);
	
		return new ResolvedInvitationParam(resolveEntities(invitation.getInvitation()), registrationCode,
				invitation.getInvitation());
	}
	
	private Set<Entity> resolveEntities(InvitationParam invitationParam)
	{
		if (invitationParam.getType().equals(InvitationType.COMBO))
		{
			try
			{
				return entityManagement.getAllEntitiesWithContactEmails(Set.of(invitationParam.getContactAddress())).stream().map(e -> e.entity).collect(Collectors.toSet());
			} catch (UnknownEmailException e)
			{
				log.debug("Email address " + invitationParam.getContactAddress() + " is unknown");
			} catch (EngineException e)
			{
				log.error("Can not get entity with contact address " + invitationParam.getContactAddress(), e);
			}
		} else if (invitationParam.getType().equals(InvitationType.ENQUIRY))
		{
			EnquiryInvitationParam enquiry = (EnquiryInvitationParam) invitationParam;
			try
			{
				return Sets.newHashSet(entityManagement.getEntity(new EntityParam(enquiry.getEntity())));
			} catch (EngineException e)
			{
				log.error("Can not get entity with id " + enquiry.getEntity(), e);
			}		
		}
		
		return Collections.emptySet();
	}

	private Optional<InvitationWithCode> getInvitationInternal(String code)
	{
		try
		{
			return Optional.of(invitationMan.getInvitation(code));
		} catch (UnknownInvitationException e)
		{
			return Optional.empty();
		} catch (EngineException e)
		{
			log.warn("Error trying to check invitation with user provided code", e);
			return Optional.empty();
		}
	}
}
