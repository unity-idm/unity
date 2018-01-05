/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.MessageTemplate;

/**
 * A {@link ComboBox} showing only the templates which are compatible with a given description.
 * @author K. Benedyczak
 */
public class CompatibleTemplatesComboBox2 extends ComboBox<String>
{
	private Collection<String> values; 
	 
	public CompatibleTemplatesComboBox2(String definitionName, MessageTemplateManagement msgTplMan) 
	{
		Map<String, MessageTemplate> templates;
		try
		{
			templates = msgTplMan.getCompatibleTemplates(definitionName);
		} catch (EngineException e)
		{
			templates = new HashMap<>();
		}
		values = templates.keySet();
		setItems(values);
	}
	
	public void setDefaultValue()
	{
		if (values != null && !values.isEmpty())
			setValue(values.iterator().next());
	}
}
