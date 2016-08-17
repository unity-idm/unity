/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * Helper class allowing to create verifiable email attributes easily.
 * @author P. Piernik
 */
public class VerifiableEmailAttribute extends Attribute
{
	public VerifiableEmailAttribute(String name, String groupPath, String value)
	{
		this(name, groupPath, EmailUtils.convertFromString(value));
	}

	/**
	 * Automatically converts {@link VerifiableEmail} parameters to strings.
	 * @param name
	 * @param groupPath
	 * @param values
	 */
	public VerifiableEmailAttribute(String name, String groupPath, VerifiableEmail... values)
	{
		super(name, VerifiableEmailAttributeSyntax.ID, groupPath, 
				Stream.of(values).
				map(v -> JsonUtil.serialize(v.toJson())).collect(Collectors.toList()));
	}
}
