/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.mconsumers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * 
 * @author P. Piernik
 * 
 */
@Component
public class SubmitFormMsgConsumer extends BaseFormMsgConsumer
{
	@Autowired
	public SubmitFormMsgConsumer(UnityMessageSource msg)
	{
		super(msg);
	}

	@Override
	public String getDescription()
	{
		return msg.getMessage("MessageTemplateConsumer.SubmitForm.desc");
	}

	@Override
	public String getName()
	{
		return "SubmitForm";
	}

	@Override
	public Map<String, String> getVariables()
	{
		Map<String, String> vars = super.getVariables();
		vars.remove("publicComment");
		vars.remove("internalComment");
		return vars;
	}

}
