/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.mocks;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attributes.AttributeValueSyntaxFactory;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

@Component
public class MockAttributeValueSyntaxFactory implements AttributeValueSyntaxFactory<String>
{

	@Override
	public AttributeValueSyntax<String> createInstance()
	{
		return new MockAttributeSyntax();
	}

	@Override
	public String getId()
	{
		return MockAttributeSyntax.ID;
	}

}
