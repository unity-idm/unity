/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.stdext.utils.EmailUtils;

/**
 * Helper class allowing to create verifiable email attributes easily.
 * @author P. Piernik
 */
public class VerifiableEmailAttribute
{
	public static Attribute of(String name, String groupPath, String value)
	{
		return of(name, groupPath, EmailUtils.convertFromString(value));
	}

	/**
	 * Automatically converts {@link VerifiableEmail} parameters to strings.
	 * @param name
	 * @param groupPath
	 * @param values
	 */
	public static Attribute of(String name, String groupPath, VerifiableEmail... values)
	{
		return new Attribute(name, VerifiableEmailAttributeSyntax.ID, groupPath, 
				Stream.of(values).
				map(v -> JsonUtil.serialize(v.toJson())).collect(Collectors.toList()));
	}
}
