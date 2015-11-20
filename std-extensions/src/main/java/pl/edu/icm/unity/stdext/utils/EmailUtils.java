/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.validator.routines.EmailValidator;

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
		
		List<String> tags = extractTags(email);
		VerifiableEmail ret = new VerifiableEmail(email);
		ret.setTags(tags);
		if (confirmed)
			ret.setConfirmationInfo(new ConfirmationInfo(true));
		return ret;
	}

	public static List<String> extractTags(String address)
	{
		String local = address.substring(0, address.indexOf('@'));
		
		String[] parts = local.split("\\+");
		if (parts.length > 1)
		{
			List<String> ret = new ArrayList<>(parts.length-1);
			for (int i=1; i<parts.length; i++)
				ret.add(parts[i]);
			return ret;
		} else
		{
			return Collections.emptyList();
		}
	}
	
	public static String removeTags(String address)
	{
		int atPos = address.indexOf('@');
		String local = address.substring(0, atPos);
		String domain = address.substring(atPos);
		
		int sepPos = local.indexOf('+');
		if (sepPos != -1)
			local = local.substring(0, sepPos);
		
		return local+domain;
	}
}
