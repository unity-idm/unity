/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.verifiers;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.JsonSerializable;

public class ConfirmationState implements JsonSerializable
{

	private String toConfirm;
	private String facilityId;
	
	
	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		// TODO Auto-generated method stub
		
	}

}
