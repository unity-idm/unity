/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation.states;

/**
 * Contains necessary informations used during the confirmation a identity
 * 
 * @author P. Piernik
 * 
 */
public class EmailIdentityConfirmationState extends UserEmailConfirmationState
{
	public static final String FACILITY_ID = "IdentityFacility";
	
	
	public EmailIdentityConfirmationState(String serializedState)
	{
		super();
		setSerializedConfiguration(serializedState);
	}

	public EmailIdentityConfirmationState(long owner, String type, String value, String locale)
	{
		super(FACILITY_ID, type, value, locale, owner);
	}
}
