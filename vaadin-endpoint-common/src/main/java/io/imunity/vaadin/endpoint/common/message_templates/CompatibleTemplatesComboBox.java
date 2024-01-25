/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.message_templates;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.combobox.ComboBox;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;

/**
 * A {@link ComboBox} showing only the templates which are compatible with a given description.
 * @author K. Benedyczak
 */
public class CompatibleTemplatesComboBox extends ComboBox<String>
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, CompatibleTemplatesComboBox.class);
	
	private Collection<String> values; 
	private final MessageTemplateManagement msgTplMan;
	 
	public CompatibleTemplatesComboBox(String definitionName, MessageTemplateManagement msgTplMan) 
	{
		this.msgTplMan = msgTplMan;
		setDefinitionName(definitionName);
	}
	
	public void setDefinitionName(String definitionName)
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
		if (values != null && values.contains(value))
			super.setValue(value);
	}
	
	public void setDefaultValue()
	{
		if (values != null && !values.isEmpty())
			setValue(values.iterator().next());
	}
}
