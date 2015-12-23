/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityRepresentation;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;


public class MockIdentity implements IdentityTypeDefinition
{
	@Override
	public String getId()
	{
		return "mockId";
	}

	@Override
	public String getDefaultDescription()
	{
		return "";
	}

	@Override
	public boolean isDynamic()
	{
		return false;
	}

	@Override
	public boolean isRemovable()
	{
		return true;
	}

	@Override
	public boolean isTargeted()
	{
		return false;
	}

	@Override
	public Set<AttributeType> getAttributesSupportedForExtraction()
	{
		return Collections.emptySet();
	}

	@Override
	public void validate(String value) throws IllegalIdentityValueException
	{
	}

	@Override
	public String getComparableValue(String from, String realm, String target)
			throws IllegalIdentityValueException
	{
		return from;
	}

	@Override
	public List<Attribute<?>> extractAttributes(String from, Map<String, String> toExtract)
	{
		return Collections.emptyList();
	}

	@Override
	public String toPrettyString(IdentityParam from)
	{
		return from.getValue();
	}

	@Override
	public String toPrettyStringNoPrefix(IdentityParam from)
	{
		return from.getValue();
	}

	@Override
	public String toString(IdentityParam from)
	{
		return from.getValue();
	}

	@Override
	public String toHumanFriendlyString(MessageSource msg, IdentityParam from)
	{
		return from.getValue();
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return "mockId";
	}

	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return "";
	}

	@Override
	public String toExternalForm(String realm, String target, String inDbValue)
			throws IllegalIdentityValueException
	{
		return inDbValue;
	}

	@Override
	public String toExternalFormNoContext(String inDbValue)
	{
		return inDbValue;
	}

	@Override
	public IdentityRepresentation createNewIdentity(String realm, String target, String value)
			throws IllegalTypeException
	{
		return new IdentityRepresentation(value, value);
	}

	@Override
	public boolean isExpired(IdentityRepresentation representation)
	{
		return false;
	}

	@Override
	public boolean isVerifiable()
	{
		return false;
	}

	@Override
	public IdentityParam convertFromString(String stringRepresentation, String remoteIdp,
			String translationProfile) throws IllegalIdentityValueException
	{
		return new IdentityParam(getId(), stringRepresentation, remoteIdp, translationProfile);
	}
}
