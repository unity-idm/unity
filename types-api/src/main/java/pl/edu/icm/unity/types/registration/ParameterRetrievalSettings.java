/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Defines in what mode the parameter should be collected
 * @author K. Benedyczak
 */
public enum ParameterRetrievalSettings
{
	/**
	 * user must enter the value manually
	 */
	interactive,
	
	/**
	 * system must set the value automatically, e.g. by extracting DN from TLS authenticated session
	 * or by taking attribute from external idp.
	 */
	automatic,
	
	/**
	 * as automatic, but the automatically collected value is not shown in the registration form
	 */
	automaticHidden,
	
	/**
	 * if system does not set value user can enter value manually, otherwise remote value is used but not shown
	 */
	automaticOrInteractive,
	
	/**
	 * system sets default value automatically but user can edit it
	 */
	automaticAndInteractive;
	
	/**
	 * @return true only if the parameter can be obtained from remote source only
	 */
	public boolean isAutomaticOnly()
	{
		return this == automatic || this == automaticHidden;
	}
	
	/**
	 * @return true only if the parameter can be obtained from remote source and should be shown.
	 */
	public boolean isPotentiallyAutomaticAndVisible()
	{
		return this == automatic;
	}
	
	public boolean isInteractivelyEntered(boolean hasRemoteValue)
	{
		return this == interactive
				|| this == automaticAndInteractive
				|| (this == automaticOrInteractive && !hasRemoteValue);
	}
}
