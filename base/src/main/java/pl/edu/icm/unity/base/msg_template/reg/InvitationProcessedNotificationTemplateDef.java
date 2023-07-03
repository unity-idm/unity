/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.msg_template.reg;

import java.util.EnumSet;
import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msg_template.MessageTemplateVariable;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;

/**
 * Template definition of a message send with an invitation to fill a registration request. 
 * @author Krzysztof Benedyczak
 */
@Component
public class InvitationProcessedNotificationTemplateDef extends BaseRegistrationTemplateDef
{
	public static final String NAME = "InvitationProcessed";
	
	public static final String CREATION_TIME = "creationTime";
	public static final String CONTACT_ADDRESS = "contactAddress";
	
	public InvitationProcessedNotificationTemplateDef()
	{
		super(NAME, "MessageTemplateConsumer.InvitationProcessedNotification.desc");
	}
	
	@Override
	public Map<String, MessageTemplateVariable> getVariables()
	{
		Map<String, MessageTemplateVariable> vars = super.getVariables();
		vars.put(CREATION_TIME, new MessageTemplateVariable(CREATION_TIME, 
				"MessageTemplateConsumer.InvitationProcessed.var.creationTime", false));
		vars.put(CONTACT_ADDRESS, new MessageTemplateVariable(CONTACT_ADDRESS, 
				"MessageTemplateConsumer.InvitationProcessed.var.contactAddress", false));
		return vars;
	}
	
	@Override
	public EnumSet<CommunicationTechnology> getCompatibleTechnologies()
	{
		return EnumSet.allOf(CommunicationTechnology.class);
	}
	
	@Override
	public boolean allowCustomVariables()
	{
		return true;
	}
}
