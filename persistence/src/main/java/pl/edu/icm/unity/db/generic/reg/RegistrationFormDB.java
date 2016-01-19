/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.db.generic.cred.CredentialHandler;
import pl.edu.icm.unity.db.generic.credreq.CredentialRequirementHandler;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.registration.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.InvitationTemplateDef;
import pl.edu.icm.unity.server.api.registration.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
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
		notificationManager.addListener(new CredentialChangeListener());
		notificationManager.addListener(new CredentialRequirementChangeListener());
		notificationManager.addListener(new GroupChangeListener());
		notificationManager.addListener(new AttributeTypeChangeListener());
		notificationManager.addListener(new MessageTemplateChangeListener());
	}
	
	private class CredentialChangeListener implements DependencyChangeListener<CredentialDefinition>
	{
		@Override
		public String getDependencyObjectType()
		{
			return CredentialHandler.CREDENTIAL_OBJECT_TYPE;
		}

		@Override
		public void preAdd(CredentialDefinition newObject, SqlSession sql) throws EngineException { }
		@Override
		public void preUpdate(CredentialDefinition oldObject,
				CredentialDefinition updatedObject, SqlSession sql) throws EngineException {}

		@Override
		public void preRemove(CredentialDefinition removedObject, SqlSession sql)
				throws EngineException
		{
			List<RegistrationForm> forms = getAll(sql);
			for (RegistrationForm form: forms)
			{
				for (CredentialRegistrationParam crParam: form.getCredentialParams())
					if (removedObject.getName().equals(crParam.getCredentialName()))
						throw new SchemaConsistencyException("The credential is used by a registration form " 
							+ form.getName());
			}
		}
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
			List<RegistrationForm> forms = getAll(sql);
			for (RegistrationForm form: forms)
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

	private class AttributeTypeChangeListener implements DependencyChangeListener<AttributeType>
	{
		@Override
		public String getDependencyObjectType()
		{
			return DBAttributes.ATTRIBUTE_TYPES_NOTIFICATION_ID;
		}

		@Override
		public void preAdd(AttributeType newObject, SqlSession sql) throws EngineException { }
		
		@Override
		public void preUpdate(AttributeType oldObject,
				AttributeType updatedObject, SqlSession sql) throws EngineException {}

		@Override
		public void preRemove(AttributeType removedObject, SqlSession sql)
				throws EngineException
		{
			List<RegistrationForm> forms = getAll(sql);
			for (RegistrationForm form: forms)
			{
				for (AttributeRegistrationParam attr: form.getAttributeParams())
					if (attr.getAttributeType().equals(removedObject.getName()))
						throw new SchemaConsistencyException("The attribute type is used by an attribute in registration form " 
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
			List<RegistrationForm> forms = getAll(sql);
			for (RegistrationForm form: forms)
			{
				RegistrationFormNotifications notCfg = form.getNotificationsConfiguration();
				if (oldObject.getName().equals(notCfg.getAcceptedTemplate()) && 
						!updatedObject.getConsumer().equals(AcceptRegistrationTemplateDef.NAME))
				{
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName() + " and the template's type change would render the template incompatible with it");
				}
				if (oldObject.getName().equals(notCfg.getRejectedTemplate()) && 
						!updatedObject.getConsumer().equals(RejectRegistrationTemplateDef.NAME))
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
				if (removedObject.getName().equals(notCfg.getAcceptedTemplate()))
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName());
				if (removedObject.getName().equals(notCfg.getRejectedTemplate()))
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName());
				if (removedObject.getName().equals(notCfg.getSubmittedTemplate()))
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName());
				if (removedObject.getName().equals(notCfg.getUpdatedTemplate()))
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName());
				if (removedObject.getName().equals(notCfg.getInvitationTemplate()))
					throw new SchemaConsistencyException("The message template is used by a registration form " 
							+ form.getName());
			}
		}
	}


}
