/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms;

import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;

/**
 *
 * @author P.Piernik
 *
 */
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

	public ResolvedInvitationParam getInvitationByCode(String registrationCode, BaseForm form) throws RegCodeException
	{
		if (registrationCode == null)
			throw new RegCodeException(ErrorCause.MISSING_CODE);
		InvitationWithCode invitation = getInvitationInternal(registrationCode).orElse(null);
		if (invitation == null)
			throw new RegCodeException(ErrorCause.UNRESOLVED_INVITATION);
		if (invitation.getInvitation().isExpired())
			throw new RegCodeException(ErrorCause.EXPIRED_INVITATION);
		try
		{
			if (!invitation.getInvitation().matchesForm(form))
				throw new RegCodeException(ErrorCause.INVITATION_OF_OTHER_FORM);
		} catch (IllegalFormTypeException e)
		{
			throw new RegCodeException(ErrorCause.UNRESOLVED_INVITATION);
		}

		return new ResolvedInvitationParam(resolveEntity(invitation.getInvitation()).orElse(null), registrationCode,
				invitation.getInvitation());
	}

	private Optional<Long> resolveEntity(InvitationParam invitationParam)
	{
		if (invitationParam.getType().equals(InvitationType.COMBO))
		{
			try
			{
				return Optional
						.of(entityManagement.getEntityByContactEmail(invitationParam.getContactAddress()).getId());
			} catch (EngineException e)
			{
				// ok
				log.debug("Email address " + invitationParam.getContactAddress() + " unknown");
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	private Optional<InvitationWithCode> getInvitationInternal(String code)
	{
		try
		{
			return Optional.of(invitationMan.getInvitation(code));
		} catch (IllegalArgumentException e)
		{
			// ok
			return Optional.empty();
		} catch (EngineException e)
		{
			log.warn("Error trying to check invitation with user provided code", e);
			return Optional.empty();
		}
	}
}
