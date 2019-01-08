/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.MessageTemplate;

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

	/**
	 * Returns a template after pre-processing (e.g. all includes are resolved).
	 * @param name
	 * @return
	 * @throws EngineException
	 */
	public MessageTemplate getPreprocessedTemplate(String name) throws EngineException;

	/**
	 * As {@link #getPreprocessedTemplate(String)} but returns the argument template after preprocessing
	 * @param toProcess
	 * @return
	 * @throws EngineException
	 */
	public MessageTemplate getPreprocessedTemplate(MessageTemplate toProcess) throws EngineException;
	
	public Map<String, MessageTemplate> getCompatibleTemplates(String templateConsumer)
			throws EngineException;
	
	void reloadFromConfiguration(Set<String> toReload);

}
