/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;

/**
 * Email which can be confirmed by user. 
 * 
 * @author P. Piernik
 */
public class VerifiableEmail implements VerifiableElement
{
	private String value;
	private ConfirmationInfo confirmationInfo;
	private List<String> tags;

	public VerifiableEmail()
	{
		this(null, new ConfirmationInfo());
	}

	public VerifiableEmail(String value)
	{
		this(value, new ConfirmationInfo());
	}

	public VerifiableEmail(String value, ConfirmationInfo confirmationData)
	{
		this.value = value == null ? null : value.trim();
		this.confirmationInfo = confirmationData;
		this.tags = value == null ? new ArrayList<>() : extractTags(value);
	}

	@JsonCreator
	public VerifiableEmail(JsonNode jsonN) throws InternalException
	{
		value = jsonN.get("value").asText();
		ConfirmationInfo confirmationData = new ConfirmationInfo();
		confirmationData.setSerializedConfiguration(jsonN.get("confirmationData").asText());
		setConfirmationInfo(confirmationData);
		List<String> tags = new ArrayList<>();
		if (jsonN.has("tags"))
		{
			ArrayNode tagsJ = (ArrayNode) jsonN.get("tags");
			for (JsonNode entry: tagsJ)
				tags.add(entry.asText());
		}
		setTags(tags);
	}
	
	@JsonValue
	public JsonNode toJson()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("value", getValue());
		main.put("confirmationData", getConfirmationInfo().getSerializedConfiguration());
		ArrayNode tagsJ = main.putArray("tags");
		for (String tag: getTags())
			tagsJ.add(tag);
		return main;
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
	
	public List<String> getTags()
	{
		return new ArrayList<>(tags);
	}

	public void setTags(List<String> tags)
	{
		this.tags = new ArrayList<>(tags);
	}
	
	public String getComparableValue()
	{
		return value == null ? null : removeTags(value);
	}
	

	public static List<String> extractTags(String address)
	{
		int atPosition = address.indexOf('@');
		if (atPosition == -1)
			return Collections.emptyList();
		String local = address.substring(0, atPosition);
		
		String[] parts = local.split("\\+");
		if (parts.length > 1)
		{
			List<String> ret = new ArrayList<>(parts.length-1);
			for (int i=1; i<parts.length; i++)
				ret.add(parts[i]);
			return ret;
		} else
		{
			return Collections.emptyList();
		}
	}
	
	public static String removeTags(String address)
	{
		int atPos = address.indexOf('@');
		String local = address.substring(0, atPos);
		String domain = address.substring(atPos);
		
		int sepPos = local.indexOf('+');
		if (sepPos != -1)
			local = local.substring(0, sepPos);
		
		return local+domain;
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
		VerifiableEmail other = (VerifiableEmail) obj;
		String cmpValue = getComparableValue();
		String otherCmpValue = other.getComparableValue();
		if (cmpValue == null)
		{
			if (otherCmpValue != null)
				return false;
		} else	if (!cmpValue.equals(otherCmpValue))
				return false;
		return true;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
