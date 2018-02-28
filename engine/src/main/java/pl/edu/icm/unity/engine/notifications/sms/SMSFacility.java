/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
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
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.utils.ContactMobileMetadataProvider;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
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
	
	@Autowired
	public SMSFacility(ExecutorsService executorsService, AttributesHelper attributesHelper)
	{
		this.executorsService = executorsService;
		this.attributesHelper = attributesHelper;
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
	public String getAddressForEntity(EntityParam recipient, String preferred)
			throws EngineException
	{
		AttributeExt mobileAttr = attributesHelper.getAttributeByMetadata(recipient, "/", 
				ContactMobileMetadataProvider.NAME);
		if (isPresent(preferred, mobileAttr))
			return preferred;
		if (mobileAttr != null)
			return mobileAttr.getValues().get(0);
		throw new IllegalIdentityValueException("The entity " + recipient + 
				" does not have the mobile number specified");
	}

	private boolean isPresent(String number, AttributeExt mobileNumAttr)
	{
		if (mobileNumAttr != null)
			for (String emailO: mobileNumAttr.getValues())
				if (emailO.equals(number))
					return true;
		return false;
	}
	
	@Override
	public String getAddressForUserRequest(UserRequestState<?> currentRequest)
			throws EngineException
	{
		Attribute mobileAttr = getMobileAttributeFromRequest(currentRequest); 
		if (mobileAttr != null)
			return mobileAttr.getValues().get(0);
		return null;
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
