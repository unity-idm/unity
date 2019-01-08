/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.utils;

import java.util.regex.Pattern;

import pl.edu.icm.unity.types.basic.VerifiableElementBase;
import pl.edu.icm.unity.types.basic.VerifiableMobileNumber;

/**
 * Mobile number utils shared
 * @author P.Piernik
 *
 */
public class MobileNumberUtils
{
	private static final int MAX_LENGTH = 15;
	public final static Pattern mobileNumberPattern = Pattern.compile("(\\+)?(\\d){3,15}");
	
	
	/**
	 * @param value
	 * @return null if ok, error string otherwise
	 */
	public static String validate(String value)
	{
		if (value == null)
			return "null value is illegal";
		if (value.length() > MAX_LENGTH)
			return "Value length (" + value.length();
		if (!mobileNumberPattern.matcher(value).matches())
			return "Value must match the " +
					"regualr expression: " + mobileNumberPattern.toString();
			
		return null;
	}
	
	public static VerifiableMobileNumber convertFromString(String stringRepresentationRaw)
	{
		VerifiableElementBase verifiableBase = ConfirmationUtils.convertFromString(stringRepresentationRaw);
		return (VerifiableMobileNumber) verifiableBase;
	}
	
}
