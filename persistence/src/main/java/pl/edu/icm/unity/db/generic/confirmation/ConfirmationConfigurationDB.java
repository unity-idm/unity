/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.confirmation;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Easy to use interface to {@link ConfirmationConfiguration} storage.
 * 
 * @author P. Piernik
 */
@Component
public class ConfirmationConfigurationDB extends GenericObjectsDB<ConfirmationConfiguration>
{
	@Autowired
	public ConfirmationConfigurationDB(ConfirmationConfigurationHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, ConfirmationConfiguration.class,
				"confirmation configuration");
		notificationManager.addListener(new AttributeTypeChangeListener());
	}

	private class AttributeTypeChangeListener implements
			DependencyChangeListener<AttributeType>
	{
		@Override
		public String getDependencyObjectType()
		{
			return DBAttributes.ATTRIBUTE_TYPES_NOTIFICATION_ID;
		}

		@Override
		public void preAdd(AttributeType newObject, SqlSession sql) throws EngineException
		{

			if (newObject.getValueType().hasValuesVerifiable() && get(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE
					+ newObject.getName(), sql) != null
					)
			{
				ConfirmationConfiguration cfg = new ConfirmationConfiguration(
						ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
						newObject.getName(), "", "");
				insert(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE
						+ newObject.getName(), cfg, sql);
			}
		}

		@Override
		public void preUpdate(AttributeType oldObject, AttributeType updatedObject,
				SqlSession sql) throws EngineException
		{
			// TODO Auto-generated method stub

		}

		@Override
		public void preRemove(AttributeType removedObject, SqlSession sql)
				throws EngineException
		{
			// TODO Auto-generated method stub

		}

	}

}
