/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.settings.msgTemplates;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for all message templates views
 * 
 * @author P.Piernik
 *
 */
@Component("MessageTemplateControllerV8")
class MessageTemplateController
{
	private MessageSource msg;
	private MessageTemplateManagement msgMan;
	private MessageTemplateConsumersRegistry consumersRegistry;
	private NotificationsManagement notChannelsMan;
	
	@Autowired
	MessageTemplateController(MessageSource msg, MessageTemplateManagement msgMan, NotificationsManagement notChannelsMan, MessageTemplateConsumersRegistry consumersRegistry)
	{
		super();
		this.msg = msg;
		this.msgMan = msgMan;
		this.consumersRegistry = consumersRegistry;
		this.notChannelsMan = notChannelsMan;
	}
	
	void addMessageTemplate(MessageTemplate toAdd) throws ControllerException

	{
		try
		{
			msgMan.addTemplate(toAdd);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("MessageTemplateController.addError", toAdd.getName()), e);
		}
	}
	
	void updateMessageTemplate(MessageTemplate toUpdate) throws ControllerException

	{
		try
		{
			msgMan.updateTemplate(toUpdate);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("MessageTemplateController.updateError", toUpdate.getName()), e);
		}
	}
	
	void removeMessageTemplates(Set<MessageTemplateEntry> items) throws ControllerException
	{
		List<String> removed = new ArrayList<>();
		try
		{
			for (MessageTemplateEntry toRemove : items)
			{
				msgMan.removeTemplate(toRemove.messageTemplate.getName());
				removed.add(toRemove.messageTemplate.getName());
			}
		} catch (Exception e)
		{
			if (removed.isEmpty())
			{
				throw new ControllerException(
						msg.getMessage("MessageTemplateController.removeError"), e);
			} else
			{
				throw new ControllerException(
						msg.getMessage("MessageTemplateController.removeError"),
						msg.getMessage("MessageTemplateController.partiallyRemoved", removed),
						e);
			}
		}
	}
	
	List<MessageTemplateEntry> getMessageTemplates() throws ControllerException
	{
		try
		{
			return msgMan.listTemplates().values().stream().map(m -> new MessageTemplateEntry(m))
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("MessageTemplateController.getAllError"), e);
		}
	}
	
	MessageTemplate getMessageTemplate(String name) throws ControllerException
	{
		try
		{
			return msgMan.getTemplate(name);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("MessageTemplateController.getError"), e);
		}
	}
	
	MessageTemplate getPreprocedMessageTemplate(MessageTemplate toProcess) throws ControllerException
	{
		try
		{
			return msgMan.getPreprocessedTemplate(toProcess);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("MessageTemplateController.getError"), e);
		}
	}
	
	void reloadFromConfiguration(Set<String> toReload) throws ControllerException
	{
		try
		{
			 msgMan.reloadFromConfiguration(toReload);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("MessageTemplateController.reloadError"), e);
		}
	}
	
	MessageTemplateEditor getEditor(MessageTemplate toEdit)
	{
		return new  MessageTemplateEditor(msg, consumersRegistry, toEdit, msgMan, notChannelsMan);
	}
	
	SimpleMessageTemplateViewer getViewer()
	{
		return new SimpleMessageTemplateViewer(msg, consumersRegistry);
	}
}
