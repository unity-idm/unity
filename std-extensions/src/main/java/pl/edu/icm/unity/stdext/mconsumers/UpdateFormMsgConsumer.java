/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.mconsumers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * 
 * @author P. Piernik
 * 
 */
@Component
public class UpdateFormMsgConsumer extends BaseFormMsgConsumer
{
	@Autowired
	public UpdateFormMsgConsumer(UnityMessageSource msg)
	{
		super(msg);
	}

	@Override
	public String getDescription()
	{
		return msg.getMessage("MessageTemplateConsumer.UpdateForm.desc");
	}

	@Override
	public String getName()
	{
		return "UpdateForm";
	}

}
