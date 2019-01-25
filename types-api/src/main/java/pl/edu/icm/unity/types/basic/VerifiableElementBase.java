/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;

/**
 * Base for all VerifiableElements. Contains value and related {@link ConfirmationInfo}
 * @author P.Piernik
 *
 */
public class VerifiableElementBase implements VerifiableElement
{
	protected String value;
	protected ConfirmationInfo confirmationInfo;

	public VerifiableElementBase()
	{
		this(null, new ConfirmationInfo());
	}

	public VerifiableElementBase(String value)
	{
		this(value, new ConfirmationInfo());
	}

	public VerifiableElementBase(String value, ConfirmationInfo confirmationData)
	{
		this.value = value == null ? null : value.trim();
		this.confirmationInfo = confirmationData;
	}

	@JsonCreator
	public VerifiableElementBase(JsonNode jsonN) throws InternalException
	{
		value = jsonN.get("value").asText();
		setConfirmationInfo(
				new ConfirmationInfo((ObjectNode) jsonN.get("confirmationData")));
	}

	@JsonValue
	public JsonNode toJson()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("value", getValue());
		main.set("confirmationData", getConfirmationInfo().toJson());
		return main;
	}

	public String toJsonString()
	{
		return JsonUtil.serialize(toJson());
	}

	@Override
	public ConfirmationInfo getConfirmationInfo()
	{
		return confirmationInfo;
	}

	@Override
	public void setConfirmationInfo(ConfirmationInfo confirmationData)
	{
		this.confirmationInfo = confirmationData;
	}

	@Override
	public String getValue()
	{
		return value;
	}

	@Override
	public boolean isConfirmed()
	{
		return confirmationInfo.isConfirmed();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VerifiableElementBase other = (VerifiableElementBase) obj;

		String otherValue = other.getValue();
		if (getValue() == null)
		{
			if (otherValue != null)
				return false;
		} else if (!value.equals(otherValue))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
