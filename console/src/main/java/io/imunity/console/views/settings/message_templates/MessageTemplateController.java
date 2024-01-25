/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.console.views.settings.message_templates;

import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateConsumersRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
class MessageTemplateController
{
	private final MessageSource msg;
	private final MessageTemplateManagement msgMan;
	private final MessageTemplateConsumersRegistry consumersRegistry;
	private final NotificationPresenter notificationPresenter;

	MessageTemplateController(MessageSource msg, MessageTemplateManagement msgMan, NotificationsManagement notChannelsMan,
							  MessageTemplateConsumersRegistry consumersRegistry, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.msgMan = msgMan;
		this.consumersRegistry = consumersRegistry;
		this.notificationPresenter = notificationPresenter;
	}
	
	void addMessageTemplate(MessageTemplate toAdd)

	{
		try
		{
			msgMan.addTemplate(toAdd);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("MessageTemplateController.addError", toAdd.getName()), e.getMessage());
		}
	}
	
	void updateMessageTemplate(MessageTemplate toUpdate)
	{
		try
		{
			msgMan.updateTemplate(toUpdate);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("MessageTemplateController.updateError", toUpdate.getName()), e.getMessage());
		}
	}
	
	void removeMessageTemplates(Set<MessageTemplateEntry> items)
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
				notificationPresenter.showError(msg.getMessage("MessageTemplateController.removeError"), e.getMessage());
			} else
			{
				notificationPresenter.showError(msg.getMessage("MessageTemplateController.removeError"), msg.getMessage("MessageTemplateController.partiallyRemoved", removed));
			}
		}
	}
	
	List<MessageTemplateEntry> getMessageTemplates()
	{
		try
		{
			return msgMan.listTemplates().values().stream()
					.map(MessageTemplateEntry::new)
					.toList();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("MessageTemplateController.getAllError"), e.getMessage());
		}
		return List.of();
	}
	
	MessageTemplate getMessageTemplate(String name)
	{
		try
		{
			return msgMan.getTemplate(name);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("MessageTemplateController.getError"), e.getMessage());
		}
		return new MessageTemplate();
	}
	
	MessageTemplate getPreprocedMessageTemplate(MessageTemplate toProcess)
	{
		try
		{
			return msgMan.getPreprocessedTemplate(toProcess);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("MessageTemplateController.getError"), e.getMessage());
		}
		return new MessageTemplate();
	}

	Set<String> getMessagesTemplatesFromConfiguration()
	{
		return msgMan.getMessagesTemplatesFromConfiguration();
	}
	
	void reloadFromConfiguration(Set<String> toReload)
	{
		try
		{
			 msgMan.reloadFromConfiguration(toReload);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("MessageTemplateController.reloadError"), e.getMessage());
		}
	}

	String getCompatibilityInformation(MessageTemplate messageTemplate)
	{
		String cons = messageTemplate.getConsumer();
		if (cons != null)
		{
			try
			{
				MessageTemplateDefinition cn = consumersRegistry.getByName(cons);
				return msg.getMessage(cn.getDescriptionKey());
			} catch (IllegalArgumentException e)
			{
				return messageTemplate.getConsumer();
			}
		}
		return "";
	}
}
