/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications.script;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.engine.notifications.email.EmailFacility;

@Component
public class GroovyEmailNotificationFacility implements NotificationFacility
{
	public static final String NAME = "SCRIPT";
	
	private final EmailFacility emailFacility;

	private ExecutorsService executorsService;
	
	@Autowired
	public GroovyEmailNotificationFacility(EmailFacility emailFacility, ExecutorsService executorsService)
	{
		this.emailFacility = emailFacility;
		this.executorsService = executorsService;
	}

	@Override
	public String getDescription()
	{
		return "Send notifications using a custom Groovy script";
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void validateConfiguration(String configuration) throws WrongArgumentException
	{
		try
		{
			JsonUtil.parse(configuration, GroovyNotificationChannelConfig.class);
		} catch (Exception e)
		{
			throw new WrongArgumentException("Channel configuration is invalid", e);
		}
	}

	@Override
	public NotificationChannelInstance getChannel(String configuration)
	{
		return new GroovyNotificationChannel(
				JsonUtil.parse(configuration, GroovyNotificationChannelConfig.class), executorsService);
	}

	@Override
	public String getAddressForEntity(EntityParam recipient, String preferred, boolean onlyConfirmed)
			throws EngineException
	{
		return emailFacility.getAddressForEntity(recipient, preferred, onlyConfirmed);
	}

	@Override
	public String getAddressForUserRequest(UserRequestState<?> currentRequest) throws EngineException
	{
		return emailFacility.getAddressForUserRequest(currentRequest);
	}

	@Override
	public CommunicationTechnology getTechnology()
	{
		return CommunicationTechnology.EMAIL;
	}
}
