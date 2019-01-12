/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.forms;

import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;

/**
 * 
 * @author P.Piernik
 *
 */
public class RegCodeException extends Exception
{
	public final ErrorCause cause;

	public RegCodeException(ErrorCause cause)
	{
		this.cause = cause;
	}


	public enum ErrorCause 
	{
		MISSING_CODE(TriggeringState.GENERAL_ERROR), 
		INVITATION_OF_OTHER_FORM(TriggeringState.GENERAL_ERROR), 
		UNRESOLVED_INVITATION(TriggeringState.INVITATION_MISSING), 
		EXPIRED_INVITATION(TriggeringState.INVITATION_EXPIRED), 
		MISCONFIGURED(TriggeringState.GENERAL_ERROR);
		
		TriggeringState triggerState;
	
		private ErrorCause(TriggeringState triggerState)
		{
			this.triggerState = triggerState;
		}
	
		public TriggeringState getTriggerState()
		{
			return triggerState;
		}
	}
}