/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;

import com.vaadin.ui.ComboBox;

/**
 * A {@link ComboBox} showing only the templates which are compatible with a given description.
 * @author K. Benedyczak
 */
public class CompatibleTemplatesComboBox extends ComboBox
{
	public CompatibleTemplatesComboBox(String definitionName, MessageTemplateManagement msgTplMan) 
	{
		Map<String, MessageTemplate> templates;
		try
		{
			templates = msgTplMan.getCompatibleTemplates(definitionName);
		} catch (EngineException e)
		{
			templates = new HashMap<>();
		}
		for (String key: templates.keySet())
		{
			addItem(key);
		}
	}
}
