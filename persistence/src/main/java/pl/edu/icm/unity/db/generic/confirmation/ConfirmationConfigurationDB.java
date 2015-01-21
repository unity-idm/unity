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
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.types.basic.AttributeType;

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
		notificationManager.addListener(new MessageTemplateChangeListener());
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

		}

		@Override
		public void preUpdate(AttributeType oldObject, AttributeType updatedObject,
				SqlSession sql) throws EngineException
		{
			if (oldObject.getValueType() == null
					|| !oldObject.getValueType().isVerifiable())
				return;
			preRemove(oldObject, sql);
		}

		@Override
		public void preRemove(AttributeType removedObject, SqlSession sql)
				throws EngineException
		{
			if (removedObject.getValueType() == null
					|| !removedObject.getValueType().isVerifiable())
				return;
			try
			{
				remove(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE
						+ removedObject.getName(), sql);
			} catch (Exception e)
			{
				// OK
			}
		}
	}

	private class MessageTemplateChangeListener implements
			DependencyChangeListener<MessageTemplate>
	{

		@Override
		public String getDependencyObjectType()
		{
			return MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE;
		}

		@Override
		public void preAdd(MessageTemplate newObject, SqlSession sql)
				throws EngineException
		{

		}

		@Override
		public void preUpdate(MessageTemplate oldObject, MessageTemplate updatedObject,
				SqlSession sql) throws EngineException
		{
			List<ConfirmationConfiguration> cfgs = getAll(sql);
			for (ConfirmationConfiguration cfg : cfgs)
			{
				if (oldObject.getName().equals(cfg.getMsgTemplate())
						&& !updatedObject.getConsumer().equals(
								ConfirmationTemplateDef.NAME))
				{
					throw new SchemaConsistencyException(
							"The message template is used by a "
									+ cfg.getNameToConfirm()
									+ " confirmation configuration  and the template's type change would render the template incompatible with it");
				}
			}
		}

		@Override
		public void preRemove(MessageTemplate removedObject, SqlSession sql)
				throws EngineException
		{
			List<ConfirmationConfiguration> cfgs = getAll(sql);
			for (ConfirmationConfiguration cfg : cfgs)
			{
				if (removedObject.getName().equals(cfg.getMsgTemplate()))
					throw new SchemaConsistencyException(
							"The message template is used by a "
									+ cfg.getNameToConfirm()
									+ "  confirmation configuration ");
			}

		}
	}
	
//For future
//	private class NotificationChannelChangeLister implements
//			DependencyChangeListener<NotificationChannel>
//	{
//
//		@Override
//		public String getDependencyObjectType()
//		{
//			return NotificationChannelHandler.NOTIFICATION_CHANNEL_ID;
//		}
//
//		@Override
//		public void preAdd(NotificationChannel newObject, SqlSession sql)
//				throws EngineException
//		{
//
//		}
//
//		@Override
//		public void preUpdate(NotificationChannel oldObject,
//				NotificationChannel updatedObject, SqlSession sql)
//				throws EngineException
//		{
//
//		}
//
//		@Override
//		public void preRemove(NotificationChannel removedObject, SqlSession sql)
//				throws EngineException
//		{
//			List<ConfirmationConfiguration> cfgs = getAll(sql);
//			for (ConfirmationConfiguration cfg : cfgs)
//			{
//				if (removedObject.getName().equals(cfg.getNotificationChannel()))
//					throw new SchemaConsistencyException(
//							"The notification channel is used by a "
//									+ cfg.getNameToConfirm()
//									+ "  confirmation configuration ");
//			}
//
//		}
//	}
}
