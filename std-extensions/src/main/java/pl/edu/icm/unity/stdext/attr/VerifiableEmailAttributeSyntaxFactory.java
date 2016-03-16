/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.attributes.AttributeValueSyntaxFactory;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

@Component
public class VerifiableEmailAttributeSyntaxFactory implements AttributeValueSyntaxFactory<VerifiableEmail>
{

	@Override
	public AttributeValueSyntax<VerifiableEmail> createInstance()
	{
		return new VerifiableEmailAttributeSyntax();
	}

	@Override
	public String getId()
	{
		return VerifiableEmailAttributeSyntax.ID;
	}

}
