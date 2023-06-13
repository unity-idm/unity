/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.util.List;

import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.DomainValidator.ArrayType;

import pl.edu.icm.unity.base.verifiable.VerifiableElementBase;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;

import org.apache.commons.validator.routines.EmailValidator;

/**
 * Email utils shared.
 * @author K. Benedyczak
 */
public class EmailUtils
{
	private static final int MAX_LENGTH = 80;
	
	private static final EmailValidator VALIDATOR;
	
	//Delta with Version 2020072301, Last Updated Fri Jul 24 07:07:01 2020 UTC
	private static final String[] ADDITIONAL_TLDS = new String[] {
			"africa",
			"amazon",
			"arab",
			"charity",
			"cpa",
			"etisalat",
			"gay",
			"grocery",
			"hotels",
			"inc",
			"llc",
			"llp",
			"map",
			"merckmsd",
			"phd",
			"rugby",
			"search",
			"sport",
			"ss",
			"xn--2scrj9c",
			"xn--3hcrj9c",
			"xn--45br5cyl",
			"xn--cckwcxetd",
			"xn--h2breg3eve",
			"xn--h2brj9c8c",
			"xn--jlq480n2rg",
			"xn--mgbaakc7dvf",
			"xn--mgbah1a3hjkrd",
			"xn--mgbai9azgqp6j",
			"xn--mgbbh1a",
			"xn--mgbcpq6gpa1a",
			"xn--mgbgu82a",
			"xn--ngbrx",
			"xn--otu796d",
			"xn--q7ce6a",
			"xn--qxa6a",
			"xn--rvc1e0am3e"
	};

	
	static 
	{
		DomainValidator.updateTLDOverride(ArrayType.GENERIC_PLUS, ADDITIONAL_TLDS);
		VALIDATOR = EmailValidator.getInstance();
	}
	
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
		if (!VALIDATOR.isValid(value))
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
