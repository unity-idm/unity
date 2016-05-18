/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.confirmation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
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
		return new GenericObjectBean(value.getName(),
				JsonUtil.serialize2Bytes(value.toJson()), supportedType);
	}

	@Override
	public ConfirmationConfiguration fromBlob(GenericObjectBean blob)
	{
		return new ConfirmationConfiguration(JsonUtil.parse(blob.getContents()));
	}

}
