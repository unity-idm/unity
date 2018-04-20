/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * Email identity type definition
 * 
 * @author P. Piernik
 */
@Component
public class EmailIdentity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "email";

	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public String getDefaultDescription()
	{
		return "Email";
	}

	@Override
	public Set<AttributeType> getAttributesSupportedForExtraction()
	{
		return Collections.emptySet();
	}

	@Override
	public void validate(String value) throws IllegalIdentityValueException
	{
		String error = EmailUtils.validate(value);
		if (error != null)
			throw new IllegalIdentityValueException(value + ": " + error);
	}
	
	@Override
	public IdentityParam convertFromString(String stringRepresentation, String remoteIdp, 
			String translationProfile) throws IllegalIdentityValueException
	{
		VerifiableEmail converted = EmailUtils.convertFromString(stringRepresentation);
		validate(converted.getValue());
		return toIdentityParam(converted, remoteIdp, translationProfile);
	}

	public static IdentityParam toIdentityParam(VerifiableEmail email, String remoteIdp, String translationProfile)
	{
		IdentityParam ret = new IdentityParam(ID, email.getValue(), remoteIdp, translationProfile);
		ret.setConfirmationInfo(email.getConfirmationInfo());
		
		return ret;
	}
	
	public static VerifiableEmail fromIdentityParam(IdentityParam idParam)
	{
		VerifiableEmail ret = new VerifiableEmail(idParam.getValue());
		if (idParam.getConfirmationInfo() != null)
			ret.setConfirmationInfo(idParam.getConfirmationInfo());
		return ret;
	}
	
	@Override
	public String getComparableValue(String from, String realm, String target)
			throws IllegalIdentityValueException
	{
		return new VerifiableEmail(from).getComparableValue();
	}

	@Override
	public List<Attribute> extractAttributes(String from, Map<String, String> toExtract)
	{
		return null;
	}

	@Override
	public String toPrettyStringNoPrefix(IdentityParam from)
	{
		VerifiableEmail ve = fromIdentityParam(from);
		StringBuilder ret = new StringBuilder(ve.getValue());
		return ret.toString();
	}

	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("EmailIdentity.description");
	}

	@Override
	public boolean isDynamic()
	{
		return false;
	}

	@Override
	public boolean isEmailVerifiable()
	{
		return true;
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("EmailIdentity.name");
	}

}
