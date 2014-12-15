/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

/**
 * Holds information about login session participant (remote IdP or relaying party). 
 * Especially useful for single logout functionality.
 * @author K. Benedyczak
 */
public interface SessionParticipant
{
	public String getProtocolType();
	public String getIdentifier();
}
