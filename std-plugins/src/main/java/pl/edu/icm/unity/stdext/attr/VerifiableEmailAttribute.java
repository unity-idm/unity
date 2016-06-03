/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.List;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create verifiable email attributes easily.
 * @author P. Piernik
 */
public class VerifiableEmailAttribute extends Attribute
{

	public VerifiableEmailAttribute(String name, String groupPath,
			List<String> values, String remoteIdp, String translationProfile)
	{
		super(name, VerifiableEmailAttributeSyntax.ID, groupPath, values, remoteIdp, translationProfile);
	}

	public VerifiableEmailAttribute(String name, String groupPath, List<String> values)
	{
		super(name, VerifiableEmailAttributeSyntax.ID, groupPath, values);
	}
}
