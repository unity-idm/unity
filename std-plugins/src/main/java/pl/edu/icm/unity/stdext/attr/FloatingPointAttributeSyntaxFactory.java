/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.base.attributes.AttributeValueSyntaxFactory;

@Component
public class FloatingPointAttributeSyntaxFactory implements AttributeValueSyntaxFactory<Double>
{
	@Override
	public AttributeValueSyntax<Double> createInstance()
	{
		return new FloatingPointAttributeSyntax();
	}

	@Override
	public String getId()
	{
		return FloatingPointAttributeSyntax.ID;
	}
	
}
