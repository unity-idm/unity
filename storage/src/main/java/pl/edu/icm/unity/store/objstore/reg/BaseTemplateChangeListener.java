/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;

/**
 * Base code for checking of form's templates consistency
 * @author K. Benedyczak
 */
public abstract class BaseTemplateChangeListener implements ReferenceRemovalHandler, 
		ReferenceUpdateHandler<MessageTemplate>
{
	protected boolean checkUpdated(BaseFormNotifications notCfg, String modifiedName, 
			MessageTemplate newValue, String formName)
	{
		if (modifiedName.equals(notCfg.getAcceptedTemplate()) && 
				!newValue.getConsumer().equals(AcceptRegistrationTemplateDef.NAME))
		{
			throw new IllegalArgumentException("The message template is used by a registration form " 
					+ formName + " and the template's type change would render the template incompatible with it");
		}
		if (modifiedName.equals(notCfg.getRejectedTemplate()) && 
				!newValue.getConsumer().equals(RejectRegistrationTemplateDef.NAME))
		{
			throw new IllegalArgumentException("The message template is used by a registration form " 
					+ formName + " and the template's type change would render the template incompatible with it");
		}
		if (modifiedName.equals(notCfg.getUpdatedTemplate()) && 
				!newValue.getConsumer().equals(UpdateRegistrationTemplateDef.NAME))
		{
			throw new IllegalArgumentException("The message template is used by a registration form " 
					+ formName + " and the template's type change would render the template incompatible with it");
		}
		
		if (modifiedName.equals(notCfg.getInvitationTemplate())
				&& !newValue.getConsumer().equals(InvitationTemplateDef.NAME))
		{
			throw new IllegalArgumentException("The message template is used by " + "a registration form "
					+ formName + " and the template's type change "
					+ "would render the template incompatible with it");
		}
		
		boolean updateNeeded = false;
		if (modifiedName.equals(newValue.getName()))
			return updateNeeded;
		
		if (modifiedName.equals(notCfg.getAcceptedTemplate()))
		{
			notCfg.setAcceptedTemplate(newValue.getName());
			updateNeeded = true;
		}
		if (modifiedName.equals(notCfg.getRejectedTemplate()))
		{
			notCfg.setRejectedTemplate(newValue.getName());
			updateNeeded = true;
		}
		if (modifiedName.equals(notCfg.getUpdatedTemplate()))
		{
			notCfg.setUpdatedTemplate(newValue.getName());
			updateNeeded = true;
		}
		
		if (modifiedName.equals(notCfg.getInvitationTemplate()))
		{
			notCfg.setInvitationTemplate(newValue.getName());
			updateNeeded = true;
		}
		
		return updateNeeded;
	}

	protected void preRemoveCheck(BaseFormNotifications notCfg, String removedName, String formName)
	{
		if (removedName.equals(notCfg.getAcceptedTemplate()))
			throw new IllegalArgumentException("The message template is used by a registration form " 
					+ formName);
		if (removedName.equals(notCfg.getRejectedTemplate()))
			throw new IllegalArgumentException("The message template is used by a registration form " 
					+ formName);
		if (removedName.equals(notCfg.getUpdatedTemplate()))
			throw new IllegalArgumentException("The message template is used by a registration form " 
					+ formName);
	}
}
