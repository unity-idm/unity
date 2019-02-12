/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.forms;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

/**
 *
 * @author P.Piernik
 *
 */
public class FormsInvitationHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, FormsInvitationHelper.class);
	
	private InvitationManagement invitationMan;
	
	public FormsInvitationHelper(InvitationManagement invitationMan)
	{
		this.invitationMan = invitationMan;
	}
	
	public InvitationParam getInvitationByCode(String registrationCode, InvitationType type) throws RegCodeException
	{
		if (registrationCode == null)
			return null;
		
		InvitationWithCode inv = getInvitationInternal(registrationCode);
		if (inv != null && inv.getInvitation() != null)
		{
			if (inv.getInvitation().getType().equals(type))
			{
				return inv.getInvitation();
			}
		}
		return null;
	}
	
	private InvitationWithCode getInvitationInternal(String code)
	{
		try
		{
			return invitationMan.getInvitation(code);
		} catch (IllegalArgumentException e)
		{
			//ok
			return null;
		} catch (EngineException e)
		{
			log.warn("Error trying to check invitation with user provided code", e);
			return null;
		}
	}
}
