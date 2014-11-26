/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.Collections;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Helper class allowing to create verifiable email attributes easily.
 * @author P. Piernik
 */
public class VerifiableEmailAttribute extends Attribute<VerifiableEmail>
{

	public VerifiableEmailAttribute(String name, String groupPath, AttributeVisibility visibility,
			VerifiableEmail value)
	{
		super(name, new VerifiableEmailAttributeSyntax(), groupPath, visibility, Collections.singletonList(value));
	}
	
	public VerifiableEmailAttribute(String name, String groupPath, AttributeVisibility visibility,
			String value)
	{		
		this(name, groupPath, visibility, new VerifiableEmail(value));
	}

}
