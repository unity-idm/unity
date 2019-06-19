/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Simple username identity type definition
 * @author K. Benedyczak
 */
@Component
public class UsernameIdentity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "userName";
	private Set<AttributeType> EXTRACTED;
	private static final String EXTRACTED_NAME = "uid";
	
	@Autowired
	public UsernameIdentity(MessageSource msg)
	{
		EXTRACTED = new HashSet<AttributeType>();
		EXTRACTED.add(new AttributeType(EXTRACTED_NAME, StringAttributeSyntax.ID, msg));
		EXTRACTED = Collections.unmodifiableSet(EXTRACTED);
	}
	
	public UsernameIdentity()
	{
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId()
	{
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDefaultDescriptionKey()
	{
		return "UsernameIdentity.description";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AttributeType> getAttributesSupportedForExtraction()
	{
		return EXTRACTED;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(String value)
	{
		if (value == null || value.trim().length() == 0)
		{
			throw new IllegalArgumentException("Username must be non empty");
		}
	}
	
	@Override
	public IdentityParam convertFromString(String stringRepresentation, String remoteIdp, 
			String translationProfile) throws IllegalIdentityValueException
	{
		return super.convertFromString(stringRepresentation.trim(), remoteIdp, translationProfile);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComparableValue(String from, String realm, String target)
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute> extractAttributes(String from, Map<String, String> toExtract)
	{
		String desiredName = toExtract.get(EXTRACTED_NAME);
		if (desiredName == null)
			return Collections.emptyList();
		return Lists.newArrayList(StringAttribute.of(desiredName, "/", from));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyStringNoPrefix(IdentityParam from)
	{
		return from.getValue();
	}


	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("UsernameIdentity.description");
	}
	
	@Override
	public boolean isDynamic()
	{
		return false;
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("UsernameIdentity.name");
	}
}
