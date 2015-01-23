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
public class IdentityConfirmationState extends BaseConfirmationState
{
	public static final String FACILITY_ID = "IdentityFacility";
	
	
	public IdentityConfirmationState(String serializedState)
	{
		super();
		setSerializedConfiguration(serializedState);
	}

	public IdentityConfirmationState(String owner, String type, String value, String locale,
			String successUrl, String errorUrl)
	{
		super(FACILITY_ID, owner, type, value, locale, successUrl, errorUrl);
	}
	
	public IdentityConfirmationState(String owner, String type, String value, String locale)
	{
		super(FACILITY_ID, owner, type, value, locale);
	}
		
	protected IdentityConfirmationState(String facilityId, String owner, String type,
			String value, String locale, String successUrl, String errorUrl)
	{
		super(facilityId, owner, type, value, locale, successUrl, errorUrl);
	}
	
	protected IdentityConfirmationState(String facilityId, String owner, String type,
			String value, String locale)
	{
		super(facilityId, owner, type, value, locale);
	}
	
	
}
