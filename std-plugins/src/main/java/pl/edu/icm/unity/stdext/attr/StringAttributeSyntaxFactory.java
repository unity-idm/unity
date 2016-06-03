/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntaxFactory;

@Component
public class StringAttributeSyntaxFactory implements AttributeValueSyntaxFactory<String>
{
	@Override
	public AttributeValueSyntax<String> createInstance()
	{
		return new StringAttributeSyntax();
	}

	@Override
	public String getId()
	{
		return StringAttributeSyntax.ID;
	}
	
}
