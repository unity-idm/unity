/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Binder;
import com.vaadin.server.ErrorMessage;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.integration.IntegrationEvent.EventType;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventConfiguration;
import pl.edu.icm.unity.engine.api.integration.Message;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.groups.OptionalGroupExcludingChildrenSelection;

/**
 * Integration event message configuration editor
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class MessageEditor extends CustomField<IntegrationEventConfiguration>
		implements IntegrationEventConfigurationEditor
{
	private BulkGroupQueryService bulkQuery;
	private AttributeSupport attributeSupport;

	private Binder<MessageVaadinBean> binder;
	private OptionalGroupExcludingChildrenSelection groupSelection;
	private String trigger;
	private CompatibleTemplatesComboBox template;
	private ChipsWithDropdown<Long> entity;
	private List<Group> groups;

	private MessageSource msg;
	private NotificationProducer notificationProducer;

	@Autowired
	MessageEditor(BulkGroupQueryService bulkQuery, MessageSource msg, AttributeSupport attributeSupport,
			MessageTemplateManagement messageTemplateManagement, NotificationProducer notificationProducer)
	{
		this.msg = msg;
		this.bulkQuery = bulkQuery;
		this.attributeSupport = attributeSupport;
		this.notificationProducer = notificationProducer;
		this.binder = new Binder<>(MessageVaadinBean.class);
		binder.setBean(new MessageVaadinBean());
		binder.addValueChangeListener(e -> {
			if (binder.isValid())
			{
				fireEvent(new ValueChangeEvent<IntegrationEventConfiguration>(this, getValue(), true));
			}
		});
		try
		{
			groups = getGroups();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("MessageEditor.getGroupsError"), e);
		}
		groupSelection = new OptionalGroupExcludingChildrenSelection(msg, true);
		groupSelection.setItems(groups);
		groupSelection.setCaption(msg.getMessage("MessageEditor.groups"));
		groupSelection.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH,
				FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		binder.forField(groupSelection).bind("groups");

		template = new CompatibleTemplatesComboBox(trigger, messageTemplateManagement);
		template.setCaption(msg.getMessage("MessageEditor.messageTemplate"));

		binder.forField(template).asRequired().bind("messageTemplate");
		Map<Long, String> entities = new HashMap<>();
		try
		{
			entities.putAll(getEntities());
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("MessageEditor.getEntitiesError"), e);
		}
		entity = new ChipsWithDropdown<>(i -> entities.get(i) + " [" + i + "]", true);
		entity.setCaption(msg.getMessage("MessageEditor.entities"));
		entity.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		entity.setItems(entities.keySet().stream().collect(Collectors.toList()));
		binder.forField(entity).bind("entities");
	}

	@Override
	public IntegrationEventConfiguration getValue()
	{
		if (binder.validate().hasErrors())
		{
			return null;
		}

		MessageVaadinBean bean = binder.getBean();
		return new Message(bean.getMessageTemplate(), bean.getEntities(),
				bean.getGroups().stream().map(g -> g.getName()).collect(Collectors.toSet()));
	}

	@Override
	public void setComponentError(ErrorMessage componentError)
	{
		binder.validate();
	}

	@Override
	public void setRequiredIndicatorVisible(boolean visible)
	{
		super.setRequiredIndicatorVisible(false);
	}

	public MessageEditor withTrigger(String trigger)
	{
		setTrigger(trigger);
		return this;
	}

	@Override
	protected Component initContent()
	{
		FormLayout main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);
		main.addComponent(template);
		main.addComponent(entity);
		main.addComponent(groupSelection);

		return main;
	}

	@Override
	public void setTrigger(String trigger)
	{
		template.setDefinitionName(trigger);
		template.setDefaultValue();
	}

	@Override
	protected void doSetValue(IntegrationEventConfiguration value)
	{
		if (value == null)
			return;
		Message message = (Message) value;
		binder.setBean(new MessageVaadinBean(message, groups));
	}

	private Map<Long, String> getEntities() throws EngineException
	{
		Map<Long, String> availableEntities = new HashMap<>();
		GroupMembershipData bulkMembershipData = bulkQuery.getBulkMembershipData("/");
		String nameAttr = getNameAttribute();
		bulkQuery.getMembershipInfo(bulkMembershipData).entrySet().stream()
				.forEach(e -> availableEntities.put(e.getKey(), getLabel(e.getValue(), nameAttr)));
		return availableEntities;
	}

	private String getLabel(EntityInGroupData info, String nameAttr)
	{
		if (nameAttr != null)
		{
			AttributeExt name = info.rootAttributesByName.get(nameAttr);
			if (name != null && !name.getValues().isEmpty())
			{
				return name.getValues().get(0);
			}
		}

		return "";
	}

	private String getNameAttribute() throws EngineException
	{
		AttributeType type = attributeSupport
				.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		if (type == null)
			return null;
		return type.getName();
	}

	private List<Group> getGroups() throws EngineException
	{
		return bulkQuery.getGroupAndSubgroups(bulkQuery.getBulkStructuralData("/")).values().stream()
				.map(groupContent -> groupContent.getGroup()).collect(Collectors.toList());
	}
	
	@Override
	public Component test(Map<String, String> params) throws EngineException
	{
		if (getValue() == null)
			return null;

		Message message = (Message) getValue();
		Collection<String> addresses = notificationProducer.sendNotification(message.groupsRecipients,
				message.singleRecipients, message.messageTemplate, params,
				msg.getDefaultLocaleCode());
		VerticalLayout mainLayout = new VerticalLayout();
		Label sentInfo = new Label();
		mainLayout.addComponent(sentInfo);
		
		if (!addresses.isEmpty())
		{
			sentInfo.setValue(msg.getMessage("MessageEditor.messageSent"));	
			VerticalLayout addrs = new VerticalLayout();
			addrs.setMargin(false);
			addrs.setSpacing(false);
			mainLayout.addComponent(addrs);
			for (String addr : addresses)
			{
				addrs.addComponent(new Label(addr));
			}
		} else
		{
			sentInfo.setValue(msg.getMessage("MessageEditor.messageNotSent"));
		}
		return mainLayout;
	}

	public static class MessageVaadinBean
	{
		private String messageTemplate;
		private List<Group> groups;
		private List<Long> entities;

		public MessageVaadinBean(Message message, List<Group> allGroups)
		{
			this.entities = message.singleRecipients;
			this.messageTemplate = message.messageTemplate;
			this.groups = new ArrayList<>();
			for (String group : message.groupsRecipients)
			{
				Optional<Group> selGroup = allGroups.stream().filter(g -> g.getName().equals(group))
						.findAny();
				if (selGroup.isPresent())
				{
					groups.add(selGroup.get());
				}
			}
		}

		public MessageVaadinBean()
		{
			groups = new ArrayList<>();
			entities = new ArrayList<>();
		}

		public String getMessageTemplate()
		{
			return messageTemplate;
		}

		public void setMessageTemplate(String messageTemplate)
		{
			this.messageTemplate = messageTemplate;
		}

		public List<Group> getGroups()
		{
			return groups;
		}

		public void setGroups(List<Group> groups)
		{
			this.groups = groups;
		}

		public List<Long> getEntities()
		{
			return entities;
		}

		public void setEntities(List<Long> entities)
		{
			this.entities = entities;
		}

	}

	@org.springframework.stereotype.Component
	public static class MessageIntegrationEventEditorFactory implements IntegrationEventConfigurationEditorFactory
	{

		private ObjectFactory<MessageEditor> factory;

		public MessageIntegrationEventEditorFactory(ObjectFactory<MessageEditor> factory)
		{
			this.factory = factory;
		}

		@Override
		public String supportedType()
		{
			return EventType.MESSAGE.toString();
		}

		@Override
		public IntegrationEventConfigurationEditor getEditor(String trigger)
		{
			return factory.getObject().withTrigger(trigger);
		}
	}
}
