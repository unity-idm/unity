/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.msg;

import java.util.List;
import java.util.Locale;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.Message;

/**
 * 
 * Management of messages
 * 
 * @author P.Piernik
 *
 */
public interface MessageManagement
{
	void addMessage(Message message) throws EngineException;
	void updateMessage(Message message) throws EngineException;
	void removeMessage(String name, Locale locale) throws EngineException;
	void removeMessage(String name) throws EngineException;
	List<Message> getAllMessages() throws EngineException;
	List<Message> getMessages(String name) throws EngineException;
}
