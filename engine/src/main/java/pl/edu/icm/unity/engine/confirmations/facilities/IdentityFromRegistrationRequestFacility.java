/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.IdentityFromRegState;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Identity from registration confirmation facility.
 * 
 * @author P. Piernik
 */
public class IdentityFromRegistrationRequestFacility implements ConfirmationFacility
{

	@Override
	public String getName()
	{
		return IdentityFromRegState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms verifiable identity from registration request";
	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		// TODO Auto-generated method stub
		return null;
	}

}
