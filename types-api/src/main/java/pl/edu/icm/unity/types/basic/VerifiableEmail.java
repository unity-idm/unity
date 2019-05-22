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

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Email which can be confirmed by user. 
 * 
 * @author P. Piernik
 */
public class VerifiableEmail extends VerifiableElementBase
{
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
		super(value, confirmationData);
		
		this.tags = value == null ? new ArrayList<>() : extractTags(value);
	}

	public static VerifiableEmail fromJsonString(String serializedValue)
	{
		return new VerifiableEmail(JsonUtil.parse(serializedValue));
	}
	
	@JsonCreator
	public VerifiableEmail(JsonNode jsonN) throws InternalException
	{
		super(jsonN);
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
		ObjectNode main = (ObjectNode) super.toJson();
		ArrayNode tagsJ = main.putArray("tags");
		for (String tag: getTags())
			tagsJ.add(tag);
		return main;
	}
	
	public List<String> getTags()
	{
		return new ArrayList<>(tags);
	}

	public void setTags(List<String> tags)
	{
		this.tags = new ArrayList<>(tags);
	}
	
	/**
	 * @return comparable value has all tags removed and is normalized to lowercase.
	 */
	public String getComparableValue()
	{
		return value == null ? null : removeTags(value).toLowerCase();
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
		if (atPos == -1)
			return address;
		String local = address.substring(0, atPos);
		String domain = address.substring(atPos);
		
		int sepPos = local.indexOf('+');
		if (sepPos != -1)
			local = local.substring(0, sepPos);
		
		return local+domain;
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
}
