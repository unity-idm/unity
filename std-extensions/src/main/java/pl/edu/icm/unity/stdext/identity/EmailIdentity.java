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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Email identity type definition
 * 
 * @author P. Piernik
 */
@Component
public class EmailIdentity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "email";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId()
	{
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDefaultDescription()
	{
		return "Email";
	}

	/**
	 * {@inheritDoc}
	 */
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
			throw new IllegalIdentityValueException(error);
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
		
		boolean main = email.getTags().contains(EmailUtils.TAG_MAIN);
		ObjectNode metadata = Constants.MAPPER.createObjectNode();
		metadata.put("main", main);
		ret.setMetadata(metadata);
			
		return ret;
	}
	
	public static VerifiableEmail fromIdentityParam(IdentityParam idParam)
	{
		VerifiableEmail ret = new VerifiableEmail(idParam.getValue());
		if (idParam.getConfirmationInfo() != null)
			ret.setConfirmationInfo(idParam.getConfirmationInfo());
		JsonNode metadata = idParam.getMetadata();
		if (metadata != null)
		{
			boolean main = false;
			if (metadata.has("main"))
				main = metadata.get("main").asBoolean();
			if (main)
				ret.addTags(EmailUtils.TAG_MAIN);
		}
		return ret;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComparableValue(String from, String realm, String target)
			throws IllegalIdentityValueException
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute<?>> extractAttributes(String from, Map<String, String> toExtract)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyStringNoPrefix(String from)
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
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
	public boolean isVerifiable()
	{
		return true;
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("EmailIdentity.name");
	}

}
