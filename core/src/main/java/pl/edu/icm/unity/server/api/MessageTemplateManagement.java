/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.notifications.MessageTemplate;

/**
 * This interface allows clients to manipulate message templates.
 * 
 * @author P. Piernik
 */
public interface MessageTemplateManagement
{
	public void addTemplate(MessageTemplate toAdd) throws EngineException;

	public void removeTemplate(String name) throws EngineException;

	public void updateTemplate(MessageTemplate updated) throws EngineException;

	public Map<String, MessageTemplate> listTemplates() throws EngineException;
	
	public MessageTemplate getTemplate(String name) throws EngineException;
}
