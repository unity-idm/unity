/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.MessageTemplate;

/**
 * A {@link ComboBox} showing only the templates which are compatible with a given description.
 * @author K. Benedyczak
 */
public class CompatibleTemplatesComboBox extends ComboBox<String>
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, CompatibleTemplatesComboBox.class);
	
	private Collection<String> values; 
	 
	public CompatibleTemplatesComboBox(String definitionName, MessageTemplateManagement msgTplMan) 
	{
		Map<String, MessageTemplate> templates = new HashMap<>();
		try
		{
			templates = msgTplMan.getCompatibleTemplates(definitionName);
		} catch (EngineException e)
		{
			LOG.error("Cannot get message templates", e);
		}
		values = templates.keySet();
		setItems(values);
	}
	
	@Override
	public void setValue(String value)
	{
		if (values.contains(value))
			super.setValue(value);
	}
	
	public void setDefaultValue()
	{
		if (values != null && !values.isEmpty())
			setValue(values.iterator().next());
	}
}
