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
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.registration.NewEnquiryTemplateDef;
import pl.edu.icm.unity.server.api.registration.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;

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
		notificationManager.addListener(new GroupChangeListener(sql -> getAll(sql)));
		notificationManager.addListener(new AttributeTypeChangeListener(sql -> getAll(sql)));
		notificationManager.addListener(new MessageTemplateChangeListener());
	}
	
	private class MessageTemplateChangeListener extends BaseTemplateChangeListener
	{
		@Override
		public void preUpdate(MessageTemplate oldObject,
				MessageTemplate updatedObject, SqlSession sql) throws EngineException 
		{
			List<EnquiryForm> forms = getAll(sql);
			for (EnquiryForm form: forms)
			{
				EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
				checkUpdated(notCfg, oldObject, updatedObject, form.getName());
				if (oldObject.getName().equals(notCfg.getEnquiryToFillTemplate()) && 
						!updatedObject.getConsumer().equals(NewEnquiryTemplateDef.NAME))
				{
					throw new SchemaConsistencyException("The message template is used by an enquiry form " 
							+ form.getName() + " and the template's type change would render the template incompatible with it");
				}
				if (oldObject.getName().equals(notCfg.getSubmittedTemplate()) && 
						!updatedObject.getConsumer().equals(SubmitRegistrationTemplateDef.NAME))
				{
					throw new SchemaConsistencyException("The message template is used by a registration form " 
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
				checkRemoved(notCfg, removedObject, form.getName());
				if (removedObject.getName().equals(notCfg.getEnquiryToFillTemplate()))
					throw new SchemaConsistencyException("The message template is used by an enquiry form " 
							+ form.getName());
				if (removedObject.getName().equals(notCfg.getSubmittedTemplate()))
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName());
			}
		}
	}
}
