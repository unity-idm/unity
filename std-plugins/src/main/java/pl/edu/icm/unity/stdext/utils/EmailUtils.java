/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.util.List;

import org.apache.commons.validator.routines.EmailValidator;

import pl.edu.icm.unity.types.basic.VerifiableElementBase;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * Email utils shared.
 * @author K. Benedyczak
 */
public class EmailUtils
{
	private static final int MAX_LENGTH = 80;
	
	/**
	 * @param value
	 * @return null if ok, error string otherwise
	 */
	public static String validate(String value)
	{
		if (value == null)
			return "null value is illegal";
		if (value.length() > MAX_LENGTH)
			return "Value length (" + value.length() 
					+ ") is too big, must be not greater than " + MAX_LENGTH;
		if (!EmailValidator.getInstance().isValid(value))
			return "Value is not a valid email address";
		if (value.startsWith("+"))
			return "Value must not start with '+', which is used to separate email tags";
		return null;
	}
	
	public static VerifiableEmail convertFromString(String stringRepresentationRaw)
	{
		VerifiableElementBase verifiableBase = ConfirmationUtils.convertFromString(stringRepresentationRaw);
		VerifiableEmail ret = new VerifiableEmail(verifiableBase.getValue(), verifiableBase.getConfirmationInfo());
	
		List<String> tags = VerifiableEmail.extractTags(verifiableBase.getValue());
		ret.setTags(tags);
	
		return ret;
	}
}
