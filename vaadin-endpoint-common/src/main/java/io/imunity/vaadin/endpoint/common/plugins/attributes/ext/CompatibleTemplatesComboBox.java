/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.combobox.ComboBox;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.MessageTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


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
		if (values.contains(value))
			super.setValue(value);
	}
	
	public void setDefaultValue()
	{
		if (values != null && !values.isEmpty())
			setValue(values.iterator().next());
	}
}
