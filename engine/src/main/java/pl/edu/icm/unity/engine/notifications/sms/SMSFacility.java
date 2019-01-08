/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications.sms;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.notifications.FacilityName;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.VerifiableMobileNumberAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.ContactMobileMetadataProvider;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.VerifiableMobileNumber;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * SMS facility allows for sending SMSes, and is configurable with the service provider.
 * I.e. we have a single SMS facility for all providers.
 * 
 * @author K. Benedyczak
 */
@Component
public class SMSFacility implements NotificationFacility
{
	public static final String NAME = FacilityName.SMS.toString();
	private ExecutorsService executorsService;
	private AttributesHelper attributesHelper;
	private AttributeTypeHelper atHelper;
	
	@Autowired
	public SMSFacility(ExecutorsService executorsService, AttributesHelper attributesHelper, AttributeTypeHelper atHelper)
	{
		this.executorsService = executorsService;
		this.attributesHelper = attributesHelper;
		this.atHelper = atHelper;
	}

	@Override
	public String getDescription()
	{
		return "Sends notifications with SMS";
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void validateConfiguration(String configuration)
	{
		parseConfig(configuration);
	}

	@Override
	public NotificationChannelInstance getChannel(String configuration)
	{
		SMSServiceProperties config = parseConfig(configuration);
		//so far only a single impl
		return new ClickatellChannel(config, executorsService);
	}

	/**
	 * Address (i.e. the number) is established from the attribute marked as contactMobile 
	 */
	@Override
	public String getAddressForEntity(EntityParam recipient, String preferred, boolean onlyConfirmed)
			throws EngineException
	{
		AttributeExt mobileAttr = attributesHelper.getAttributeByMetadata(recipient, "/", 
				ContactMobileMetadataProvider.NAME);
		if (isPresent(preferred, mobileAttr, onlyConfirmed))
			return preferred;
		
		String confirmedNumber = getAddressFrom(mobileAttr, true);
		if (confirmedNumber != null)
			return confirmedNumber;
		
		if (!onlyConfirmed)
		{
			String plain = getAddressFrom(mobileAttr, false);
			if (plain != null)
				return plain;

		}
		throw new IllegalIdentityValueException("The entity " + recipient
				+ " does not have the" + (onlyConfirmed ? " confirmed" : "")
				+ " mobile number specified");
	}

	private boolean isPresent(String number, AttributeExt mobileNumAttr, boolean onlyConfirmed)
	{
		if (mobileNumAttr != null)
		{
			for (String mobile : mobileNumAttr.getValues())
			{
				if (mobile.equals(number))
				{

					if (onlyConfirmed)
					{
						AttributeValueSyntax<?> syntax = atHelper
								.getUnconfiguredSyntax(mobileNumAttr
										.getValueSyntax());
						if (syntax.getValueSyntaxId().equals(
								VerifiableMobileNumberAttributeSyntax.ID))
						{
							VerifiableMobileNumber vmobile = (VerifiableMobileNumber) syntax
									.convertFromString(mobile);
							if (vmobile.isConfirmed())
								return true;
						}

					} else
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	
	private String getAddressFrom(Attribute mobileAttr, boolean useConfirmed)
	{

		if (mobileAttr != null)
		{
			AttributeValueSyntax<?> syntax = atHelper
					.getUnconfiguredSyntax(mobileAttr.getValueSyntax());
			if (!useConfirmed || syntax.getValueSyntaxId()
					.equals(VerifiableMobileNumberAttributeSyntax.ID))
			{
				for (String mobileO : mobileAttr.getValues())
				{
					if (syntax.getValueSyntaxId().equals(
							VerifiableMobileNumberAttributeSyntax.ID))
					{
						VerifiableMobileNumber mobile = (VerifiableMobileNumber) syntax
								.convertFromString(mobileO);
						if (!useConfirmed || mobile.isConfirmed())
							return mobile.getValue();
					} else if (!useConfirmed)
					{
						return mobileO.toString();
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public String getAddressForUserRequest(UserRequestState<?> currentRequest)
			throws EngineException
	{
		Attribute mobileAttr = getMobileAttributeFromRequest(currentRequest); 
		return getAddressFrom(mobileAttr, false);
	}

	private Attribute getMobileAttributeFromRequest(UserRequestState<?> currentRequest)
			throws EngineException
	{
		List<Attribute> attrs = currentRequest.getRequest().getAttributes();
		if (attrs == null)
			return null;
		AttributeType at = attributesHelper.getAttributeTypeWithSingeltonMetadata(
				ContactMobileMetadataProvider.NAME);
		if (at == null)
			return null;
		for (Attribute ap : attrs)
		{
			if (ap == null)
				continue;
			if (ap.getName().equals(at.getName()) && ap.getGroupPath().equals("/"))
				return ap;
		}
		return null;
	}
	
	private SMSServiceProperties parseConfig(String configuration)
	{
		try
		{
			Properties props = new Properties();
			props.load(new StringReader(configuration));
			return new SMSServiceProperties(props);
		} catch (IOException e)
		{
			throw new ConfigurationException("SMS configuration is invalid: " +
					"not a valid properties syntax was used", e);
		}
	}
}
