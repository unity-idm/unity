/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.stdext.utils.MobileNumberUtils;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.VerifiableMobileNumber;

/**
 * Helper class allowing to create verifiable mobile number attributes easily.
 * @author P. Piernik
 */
public class VerifiableMobileNumberAttribute
{
	public static Attribute of(String name, String groupPath, String value)
	{
		return of(name, groupPath, MobileNumberUtils.convertFromString(value));
	}

	/**
	 * Automatically converts {@link VerifiableMobileNumber} parameters to strings.
	 * @param name
	 * @param groupPath
	 * @param values
	 */
	public static Attribute of(String name, String groupPath, VerifiableMobileNumber... values)
	{
		return new Attribute(name, VerifiableMobileNumberAttributeSyntax.ID, groupPath, 
				Stream.of(values).
				map(v -> JsonUtil.serialize(v.toJson())).collect(Collectors.toList()));
	}
}
