/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.utils;

import pl.edu.icm.unity.types.basic.VerifiableElementBase;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

public class ConfirmationUtils
{
	public final static String CONFIRMED_POSTFIX = "[CONFIRMED]"; 
	public final static String UNCONFIRMED_POSTFIX = "[UNCONFIRMED]"; 
	
	
	public static VerifiableElementBase convertFromString(String stringRepresentationRaw)
	{
		String stringRepresentation = stringRepresentationRaw.trim();
		String stringRepresentationTrimmed = stringRepresentation;
		boolean confirmed = false;
		if (stringRepresentation.endsWith(CONFIRMED_POSTFIX))
		{
			confirmed = true;
			stringRepresentationTrimmed = stringRepresentation.substring(0, stringRepresentation.length() - 
					CONFIRMED_POSTFIX.length());
		}
		if (stringRepresentation.endsWith(UNCONFIRMED_POSTFIX))
		{
			confirmed = false;
			stringRepresentationTrimmed = stringRepresentation.substring(0, stringRepresentation.length() - 
					UNCONFIRMED_POSTFIX.length());
		}
		
		VerifiableElementBase ret = new VerifiableElementBase(stringRepresentationTrimmed);
		if (confirmed)
			ret.setConfirmationInfo(new ConfirmationInfo(true));
		return ret;
	}	
}
