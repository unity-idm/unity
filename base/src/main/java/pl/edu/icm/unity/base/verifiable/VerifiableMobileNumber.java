/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.verifiable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.JsonUtil;

/**
 * Mobile number which can be confirmed by user. 
 * 
 * @author P. Piernik
 */
public class VerifiableMobileNumber extends VerifiableElementBase
{

	public VerifiableMobileNumber()
	{
		this(null, new ConfirmationInfo());
	}

	public VerifiableMobileNumber(String value)
	{
		this(value, new ConfirmationInfo());
	}

	public VerifiableMobileNumber(String value, ConfirmationInfo confirmationData)
	{
		super(value, confirmationData);
	}
	
	@JsonCreator
	public VerifiableMobileNumber(JsonNode jsonN) throws InternalException
	{
		super(jsonN);
	}

	public static VerifiableMobileNumber fromJsonString(String serializedValue)
	{
		return new VerifiableMobileNumber(JsonUtil.parse(serializedValue));
	}
}
