/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.util.List;

import org.apache.commons.validator.routines.EmailValidator;

import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Email utils shared.
 * @author K. Benedyczak
 */
public class EmailUtils
{
	private static final int MAX_LENGTH = 80;
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
		if (!EmailValidator.getInstance().isValid(value))
			return "Value is not a valid email address";
		if (value.startsWith("+"))
			return "Value must not start with '+', which is used to separate email tags";
		return null;
	}
	
	public static VerifiableEmail convertFromString(String stringRepresentationRaw)
	{
		String stringRepresentation = stringRepresentationRaw.trim();
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
		
		List<String> tags = VerifiableEmail.extractTags(email);
		VerifiableEmail ret = new VerifiableEmail(email);
		ret.setTags(tags);
		if (confirmed)
			ret.setConfirmationInfo(new ConfirmationInfo(true));
		return ret;
	}
}
