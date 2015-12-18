/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

/**
 * Describes registration context, i.e. circumstances and environment at the request submission.
 * This data influences the submitted request's processing.
 * @author K. Benedyczak
 */
public class RegistrationContext
{
	/**
	 * Describes how the registration form was triggered.
	 * @author K. Benedyczak
	 */
	public enum TriggeringMode {
		/**
		 * User selected registration manually on one of login screens
		 */
		manualAtLogin, 
		
		/**
		 * User entered a well-known registration form link
		 */
		manualStandalone, 
		
		/**
		 * Form was shown after a successful remote authentication 
		 * which was not mapped to a local entity by an input transaltion profile. 
		 */
		afterRemoteLogin
	}
	
	public final boolean tryAutoAccept;
	public final boolean isOnIdpEndpoint;
	public final TriggeringMode triggeringMode;
	
	public RegistrationContext(boolean tryAutoAccept, boolean isOnIdpEndpoint,
			TriggeringMode triggeringMode)
	{
		this.tryAutoAccept = tryAutoAccept;
		this.isOnIdpEndpoint = isOnIdpEndpoint;
		this.triggeringMode = triggeringMode;
	}
}
