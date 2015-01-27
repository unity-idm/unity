/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.confirmation;

import java.nio.charset.StandardCharsets;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;

import com.fasterxml.jackson.databind.ObjectMapper;

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
	public GenericObjectBean toBlob(ConfirmationConfiguration value, SqlSession sql)
	{
		String json = value.toJson(jsonMapper);
		return new GenericObjectBean(value.getTypeToConfirm() + value.getNameToConfirm(),
				json.getBytes(StandardCharsets.UTF_8), supportedType);
	}

	@Override
	public ConfirmationConfiguration fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		return new ConfirmationConfiguration(new String(blob.getContents()), jsonMapper);
	}

}
