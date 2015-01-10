/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Email identity type definition
 * 
 * @author P. Piernik
 */
@Component
public class EmailIdentity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "email";
	private final int MIN_LENGTH = 5;
	private final int MAX_LENGTH = 33;
	private final String EMAIL_REGEXP = "[^@]+@.+\\..+";

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
	public String getDefaultDescription()
	{
		return "Email";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AttributeType> getAttributesSupportedForExtraction()
	{
		return null;
	}

	@Override
	public void validate(String value) throws IllegalIdentityValueException
	{
		Pattern pattern = Pattern.compile(EMAIL_REGEXP);
		if (value == null)
			throw new IllegalIdentityValueException("null value is illegal");
		if (value.length() < MIN_LENGTH)
			throw new IllegalIdentityValueException("Value length (" + value.length()
					+ ") is too small, must be at least " + MIN_LENGTH);
		if (value.length() > MAX_LENGTH)
			throw new IllegalIdentityValueException("Value length (" + value.length()
					+ ") is too big, must be not greater than " + MAX_LENGTH);
		if (pattern != null)
			if (!pattern.matcher(value).matches())
				throw new IllegalIdentityValueException("Value must match the "
						+ "regualr expression: " + EMAIL_REGEXP);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComparableValue(String from, String realm, String target)
			throws IllegalIdentityValueException
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute<?>> extractAttributes(String from, Map<String, String> toExtract)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyStringNoPrefix(String from)
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("EmailIdentity.description");
	}

	@Override
	public boolean isDynamic()
	{
		return false;
	}

	@Override
	public boolean isVerifiable()
	{
		return true;
	}

}
