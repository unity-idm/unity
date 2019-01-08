/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * 
 * @author K. Benedyczak
 */
@Component
public class ContactEmailMetadataProvider implements AttributeMetadataProvider 
{
	public static final String NAME = "contactEmail";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "The value of attribute in the root group, " +
				"marked with this designator is used by system as the contact email address of the entity.";
	}

	@Override
	public void verify(String metadata, AttributeType at) throws IllegalAttributeTypeException
	{
		if (!(StringAttributeSyntax.ID.equals(at.getValueSyntax()) ||
				VerifiableEmailAttributeSyntax.ID.equals(at.getValueSyntax())))
			throw new IllegalAttributeTypeException("The " + NAME + " designator can be applied only " +
					"to string or verifiableEmail type attribute types.");
		if (at.getMinElements() == 0)
			throw new IllegalAttributeTypeException("The " + NAME + " designator can be applied only " +
					"to attribute types with at least one mandatory value.");
		if (!"".equals(metadata))
			throw new IllegalAttributeTypeException("The " + NAME + " designator must have an empty value");
	}

	@Override
	public boolean isSingleton()
	{
		return true;
	}

	@Override
	public boolean isSecuritySensitive()
	{
		return true;
	}
}
