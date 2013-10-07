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
	automaticHidden
}
