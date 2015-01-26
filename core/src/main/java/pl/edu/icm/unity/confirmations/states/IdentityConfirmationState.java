/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;



/**
 * Contains necessary informations used during the confirmation a identity
 * 
 * @author P. Piernik
 * 
 */
public class IdentityConfirmationState extends UserConfirmationState
{
	public static final String FACILITY_ID = "IdentityFacility";
	
	
	public IdentityConfirmationState(String serializedState)
	{
		super();
		setSerializedConfiguration(serializedState);
	}

	public IdentityConfirmationState(long owner, String type, String value, String locale,
			String successUrl, String errorUrl)
	{
		super(FACILITY_ID, type, value, locale, successUrl, errorUrl, owner);
	}
	
	public IdentityConfirmationState(long owner, String type, String value, String locale)
	{
		super(FACILITY_ID, type, value, locale, owner);
	}
}
