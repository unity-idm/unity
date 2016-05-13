/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.confirmation;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

/**
 * Handler for {@link ConfirmationConfiguration}.
 * 
 * @author P. Piernik
 */
@Component
public class ConfirmationConfigurationHandler extends
		DefaultEntityHandler<ConfirmationConfiguration>
{
	public static final String CONFIRMATION_CONFIGURATION_OBJECT_TYPE = "confirmationConfiguration";

	@Autowired
	public ConfirmationConfigurationHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, CONFIRMATION_CONFIGURATION_OBJECT_TYPE,
				ConfirmationConfiguration.class);
	}

	@Override
	public GenericObjectBean toBlob(ConfirmationConfiguration value)
	{
		String json = value.toJson(jsonMapper);
		return new GenericObjectBean(value.getName(),
				json.getBytes(StandardCharsets.UTF_8), supportedType);
	}

	@Override
	public ConfirmationConfiguration fromBlob(GenericObjectBean blob)
	{
		return new ConfirmationConfiguration(new String(blob.getContents(), StandardCharsets.UTF_8), jsonMapper);
	}

}
