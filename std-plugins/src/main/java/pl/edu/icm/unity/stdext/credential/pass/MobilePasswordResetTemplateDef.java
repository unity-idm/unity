/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.util.EnumSet;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;

/**
 * Defines template used for sending password reset confirmation code by mobile.
 * @author P. Piernik
 * 
 */
@Component
public class MobilePasswordResetTemplateDef extends PasswordResetTemplateDefBase implements MessageTemplateDefinition
{
	public static final String NAME = "MobilePasswordResetCode";
	
	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.MobilePasswordReset.desc";
	}

	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public EnumSet<CommunicationTechnology> getCompatibleTechnologies()
	{
		 return EnumSet.of(CommunicationTechnology.SMS);
	}
}
