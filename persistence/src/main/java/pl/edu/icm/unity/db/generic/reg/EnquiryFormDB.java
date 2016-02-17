/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.registration.EnquiryFilledTemplateDef;
import pl.edu.icm.unity.server.api.registration.NewEnquiryTemplateDef;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;

/**
 * Easy access to {@link EnquiryForm} storage.
 * @author K. Benedyczak
 */
@Component
public class EnquiryFormDB extends GenericObjectsDB<EnquiryForm>
{
	@Autowired
	public EnquiryFormDB(EnquiryFormHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, EnquiryForm.class,
				"enquiry form");
		notificationManager.addListener(new CredentialChangeListener(sql -> getAll(sql)));
		notificationManager.addListener(new GroupChangeListener());
		notificationManager.addListener(new AttributeTypeChangeListener(sql -> getAll(sql)));
		notificationManager.addListener(new MessageTemplateChangeListener());
	}
	
	private class GroupChangeListener implements DependencyChangeListener<Group>
	{
		@Override
		public String getDependencyObjectType()
		{
			return DBGroups.GROUPS_NOTIFICATION_ID;
		}

		@Override
		public void preAdd(Group newObject, SqlSession sql) throws EngineException { }
		@Override
		public void preUpdate(Group oldObject,
				Group updatedObject, SqlSession sql) throws EngineException {}

		@Override
		public void preRemove(Group removedObject, SqlSession sql)
				throws EngineException
		{
			List<EnquiryForm> forms = getAll(sql);
			for (EnquiryForm form: forms)
			{
				for (GroupRegistrationParam group: form.getGroupParams())
					if (group.getGroupPath().startsWith(removedObject.toString()))
						throw new SchemaConsistencyException("The group is used by a registration form " 
							+ form.getName());
				for (AttributeRegistrationParam attr: form.getAttributeParams())
					if (attr.getGroup().startsWith(removedObject.toString()))
						throw new SchemaConsistencyException("The group is used by an attribute in registration form " 
							+ form.getName());
				if (form.getNotificationsConfiguration() != null && 
						removedObject.toString().equals(form.getNotificationsConfiguration().
								getAdminsNotificationGroup()))
					throw new SchemaConsistencyException("The group is used as administrators notification group in registration form " 
							+ form.getName());
			}
		}
	}

	private class MessageTemplateChangeListener implements DependencyChangeListener<MessageTemplate>
	{
		@Override
		public String getDependencyObjectType()
		{
			return MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE;
		}

		@Override
		public void preAdd(MessageTemplate newObject, SqlSession sql) throws EngineException { }
		@Override
		public void preUpdate(MessageTemplate oldObject,
				MessageTemplate updatedObject, SqlSession sql) throws EngineException 
		{
			List<EnquiryForm> forms = getAll(sql);
			for (EnquiryForm form: forms)
			{
				EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
				if (oldObject.getName().equals(notCfg.getEnquiryFilledTemplate()) && 
						!updatedObject.getConsumer().equals(EnquiryFilledTemplateDef.NAME))
				{
					throw new SchemaConsistencyException("The message template is used by an enquiry form " 
							+ form.getName() + " and the template's type change would render the template incompatible with it");
				}
				if (oldObject.getName().equals(notCfg.getEnquiryFilledTemplate()) && 
						!updatedObject.getConsumer().equals(NewEnquiryTemplateDef.NAME))
				{
					throw new SchemaConsistencyException("The message template is used by an enquiry form " 
							+ form.getName() + " and the template's type change would render the template incompatible with it");
				}
			}
			
		}

		@Override
		public void preRemove(MessageTemplate removedObject, SqlSession sql)
				throws EngineException
		{
			List<EnquiryForm> forms = getAll(sql);
			for (EnquiryForm form: forms)
			{
				EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
				if (removedObject.getName().equals(notCfg.getEnquiryFilledTemplate()))
					throw new SchemaConsistencyException("The message template is used by an enquiry form " 
							+ form.getName());
				if (removedObject.getName().equals(notCfg.getEnquiryToFillTemplate()))
					throw new SchemaConsistencyException("The message template is used by an enquiry form " 
							+ form.getName());
			}
		}
	}


}
