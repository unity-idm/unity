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
public abstract class BaseFormMsgConsumer implements MessageTemplateConsumer
{
	protected UnityMessageSource msg;

	@Autowired
	public BaseFormMsgConsumer(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getDescription()
	{
		return "";
	}

	@Override
	public String getName()
	{
		return "";
	}

	@Override
	public Map<String, String> getVariables()
	{
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("formName",
				msg.getMessage("MessageTemplateConsumer.BaseForm.var.formName"));
		vars.put("requestId",
				msg.getMessage("MessageTemplateConsumer.BaseForm.var.requestId"));
		vars.put("publicComment", msg
				.getMessage("MessageTemplateConsumer.BaseForm.var.publicComment"));
		vars.put("internalComment", msg
				.getMessage("MessageTemplateConsumer.BaseForm.var.internalComment"));
		return vars;
	}

}
