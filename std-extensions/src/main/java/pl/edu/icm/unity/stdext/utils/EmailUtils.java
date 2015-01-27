/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.util.regex.Pattern;

import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Email utils shared by {@link EmailIdentity} and {@link VerifiableEmailAttributeSyntax}.
 * @author K. Benedyczak
 */
public class EmailUtils
{
	private static final int MAX_LENGTH = 80;
	private static final String EMAIL_REGEXP = "[^@]+@.+\\..+";
	private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEXP);
	
	public final static String CONFIRMED_POSTFIX = "[CONFIRMED]"; 
	public final static String UNCONFIRMED_POSTFIX = "[UNCONFIRMED]"; 
	
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
		if (!PATTERN.matcher(value).matches())
			return "Value must match the regualr expression: " + EMAIL_REGEXP;
		return null;
	}
	
	public static VerifiableEmail convertFromString(String stringRepresentation)
	{
		String email = stringRepresentation;
		boolean confirmed = false;
		if (stringRepresentation.endsWith(CONFIRMED_POSTFIX))
		{
			confirmed = true;
			email = stringRepresentation.substring(0, stringRepresentation.length() - 
					CONFIRMED_POSTFIX.length());
		}
		if (stringRepresentation.endsWith(UNCONFIRMED_POSTFIX))
		{
			confirmed = false;
			email = stringRepresentation.substring(0, stringRepresentation.length() - 
					UNCONFIRMED_POSTFIX.length());
		}
		
			
		VerifiableEmail ret = new VerifiableEmail(email);
		if (confirmed)
			ret.setConfirmationInfo(new ConfirmationInfo(true));
		return ret;
	}

}
