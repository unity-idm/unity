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
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.db.generic.credreq.CredentialRequirementHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.registration.InvitationTemplateDef;
import pl.edu.icm.unity.server.api.registration.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;

/**
 * Easy access to {@link RegistrationForm} storage.
 * @author K. Benedyczak
 */
@Component
public class RegistrationFormDB extends GenericObjectsDB<RegistrationForm>
{
	@Autowired
	public RegistrationFormDB(RegistrationFormHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, RegistrationForm.class,
				"registration form");
		notificationManager.addListener(new CredentialChangeListener(sql -> getAll(sql)));
		notificationManager.addListener(new CredentialRequirementChangeListener());
		notificationManager.addListener(new GroupChangeListener(sql -> getAll(sql)));
		notificationManager.addListener(new AttributeTypeChangeListener(sql -> getAll(sql)));
		notificationManager.addListener(new MessageTemplateChangeListener());
	}
	
	private class CredentialRequirementChangeListener implements DependencyChangeListener<CredentialRequirements>
	{
		@Override
		public String getDependencyObjectType()
		{
			return CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE;
		}

		@Override
		public void preAdd(CredentialRequirements newObject, SqlSession sql) throws EngineException { }
		@Override
		public void preUpdate(CredentialRequirements oldObject,
				CredentialRequirements updatedObject, SqlSession sql) throws EngineException {}

		@Override
		public void preRemove(CredentialRequirements removedObject, SqlSession sql)
				throws EngineException
		{
			List<RegistrationForm> forms = getAll(sql);
			for (RegistrationForm form: forms)
			{
				if (removedObject.getName().equals(form.getDefaultCredentialRequirement()))
					throw new SchemaConsistencyException("The credential requirement is used by a registration form " 
							+ form.getName());
			}
		}
	}

	private class MessageTemplateChangeListener extends BaseTemplateChangeListener
	{
		@Override
		public void preUpdate(MessageTemplate oldObject,
				MessageTemplate updatedObject, SqlSession sql) throws EngineException 
		{
			List<RegistrationForm> forms = getAll(sql);
			for (RegistrationForm form: forms)
			{
				RegistrationFormNotifications notCfg = form.getNotificationsConfiguration();
				checkUpdated(notCfg, oldObject, updatedObject, form.getName());
				if (oldObject.getName().equals(notCfg.getUpdatedTemplate()) && 
						!updatedObject.getConsumer().equals(UpdateRegistrationTemplateDef.NAME))
				{
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName() + " and the template's type change would render the template incompatible with it");
				}
				if (oldObject.getName().equals(notCfg.getInvitationTemplate()) && 
						!updatedObject.getConsumer().equals(InvitationTemplateDef.NAME))
				{
					throw new SchemaConsistencyException("The message template is used by a registration form " 
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
			List<RegistrationForm> forms = getAll(sql);
			for (RegistrationForm form: forms)
			{
				RegistrationFormNotifications notCfg = form.getNotificationsConfiguration();
				checkRemoved(notCfg, removedObject, form.getName());
				if (removedObject.getName().equals(notCfg.getUpdatedTemplate()))
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName());
				if (removedObject.getName().equals(notCfg.getInvitationTemplate()))
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName());
				if (removedObject.getName().equals(notCfg.getSubmittedTemplate()))
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName());
			}
		}
	}
}
