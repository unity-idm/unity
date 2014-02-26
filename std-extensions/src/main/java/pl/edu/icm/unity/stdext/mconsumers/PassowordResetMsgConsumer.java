/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.mconsumers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.msgtemplates.MessageTemplateConsumer;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * 
 * @author P. Piernik
 * 
 */
@Component
public class PassowordResetMsgConsumer implements MessageTemplateConsumer
{
	private UnityMessageSource msg;

	@Autowired
	public PassowordResetMsgConsumer(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getDescription()
	{
		return msg.getMessage("MessageTemplateConsumer.PasswordReset.desc");
	}

	@Override
	public String getName()
	{
		return "PasswordResetCode";
	}

	@Override
	public Map<String, String> getVariables()
	{
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("user", msg.getMessage("MessageTemplateConsumer.PasswordReset.var.user"));
		vars.put("code", msg.getMessage("MessageTemplateConsumer.PasswordReset.var.code"));
		return vars;
	}

}
