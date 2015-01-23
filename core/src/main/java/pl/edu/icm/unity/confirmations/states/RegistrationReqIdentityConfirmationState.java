/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;


/**
 * Contains necessary informations used during the confirmation a identity in
 * registration request
 * 
 * @author P. Piernik
 * 
 */
public class RegistrationReqIdentityConfirmationState extends IdentityConfirmationState
{
	public static final String FACILITY_ID = "RegistrationReqIdentityFacility";

	public RegistrationReqIdentityConfirmationState(String serializedState)
	{
		super(serializedState);
	}

	public RegistrationReqIdentityConfirmationState(String owner, String type,
			String value, String locale, String successUrl, String errorUrl)
	{
		super(FACILITY_ID, owner, type, value, locale, successUrl, errorUrl);
	}
	
	public RegistrationReqIdentityConfirmationState(String owner, String type,
			String value, String locale)
	{
		super(FACILITY_ID, owner, type, value, locale);
	}
}
