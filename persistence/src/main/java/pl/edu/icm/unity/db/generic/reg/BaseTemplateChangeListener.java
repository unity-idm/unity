/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.registration.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;

/**
 * Base code for checking of form's templates consistency
 * @author K. Benedyczak
 */
public abstract class BaseTemplateChangeListener implements DependencyChangeListener<MessageTemplate>
{
	@Override
	public String getDependencyObjectType()
	{
		return MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE;
	}

	@Override
	public void preAdd(MessageTemplate newObject, SqlSession sql) throws EngineException { }
	
	protected void checkUpdated(BaseFormNotifications notCfg, MessageTemplate oldObject,
			MessageTemplate updatedObject, String formName) throws SchemaConsistencyException 
	{
		if (oldObject.getName().equals(notCfg.getAcceptedTemplate()) && 
				!updatedObject.getConsumer().equals(AcceptRegistrationTemplateDef.NAME))
		{
			throw new SchemaConsistencyException("The message template is used by a registration form " 
					+ formName + " and the template's type change would render the template incompatible with it");
		}
		if (oldObject.getName().equals(notCfg.getRejectedTemplate()) && 
				!updatedObject.getConsumer().equals(RejectRegistrationTemplateDef.NAME))
		{
			throw new SchemaConsistencyException("The message template is used by a registration form " 
					+ formName + " and the template's type change would render the template incompatible with it");
		}
	}

	protected void checkRemoved(BaseFormNotifications notCfg, MessageTemplate removedObject, String formName) 
			throws SchemaConsistencyException
	{
			if (removedObject.getName().equals(notCfg.getAcceptedTemplate()))
				throw new SchemaConsistencyException("The message template is used by a registration form " 
						+ formName);
			if (removedObject.getName().equals(notCfg.getRejectedTemplate()))
				throw new SchemaConsistencyException("The message template is used by a registration form " 
						+ formName);
	}
}
