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
	public static final String FACILITY_ID = "identityFacility";
	
	@Override
	public String getFacilityId()
	{
		return FACILITY_ID;
	}
	
}
