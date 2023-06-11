/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.integration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.integration.IntegrationEvent;
import pl.edu.icm.unity.engine.api.integration.IntegrationEvent.EventType;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventGroup;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;

/**
 * Represents list of editable integration events
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class IntegrationEventsEditor extends CustomComponent
{
	private MessageSource msg;
	private ObjectFactory<IntegrationEventEditorComponent> factory;
	private VerticalLayout eventLayout;
	private List<IntegrationEventEditorComponent> events;
	private List<IntegrationEventGroup> eventsGroups;

	@Autowired
	public IntegrationEventsEditor(MessageSource msg, ObjectFactory<IntegrationEventEditorComponent> factory)
	{
		this.msg = msg;
		this.factory = factory;
		this.events = new ArrayList<>();
		this.eventsGroups = new ArrayList<>();
		initUI();
	}

	public IntegrationEventsEditor forGroups(List<IntegrationEventGroup> groups)
	{
		eventsGroups.addAll(groups);
		return this;
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);

		eventLayout = new VerticalLayout();
		eventLayout.setSpacing(false);
		eventLayout.setMargin(false);
		eventLayout.setHeightUndefined();
		eventLayout.setWidth(100, Unit.PERCENTAGE);

		HorizontalLayout buttonsBar = StandardButtonsHelper
				.buildTopButtonsBar(StandardButtonsHelper.build4AddAction(msg,
						e -> addEvent(new IntegrationEvent(
								msg.getMessage("IntegrationEventsEditor.new"),
								EventType.WEBHOOK), true)));
		main.addComponent(buttonsBar);
		main.addComponent(eventLayout);

		setCompositionRoot(main);
	}

	public void setValue(List<IntegrationEvent> toEdit)
	{
		events.clear();
		toEdit.forEach(ie -> addEvent(ie, false));
	}

	private void addEvent(IntegrationEvent webhook, boolean expand)
	{
		IntegrationEventEditorComponent webhookComponent = factory.getObject().withCallback(l -> {
			eventLayout.removeComponent(l);
			events.remove(l);
		}).withEvent(webhook).forWebhookGroup(eventsGroups);
		if (expand)
		{
			webhookComponent.expand();
			webhookComponent.focus();
		}
		events.add(webhookComponent);
		eventLayout.addComponent(webhookComponent);
	}

	public List<IntegrationEvent> getValue() throws FormValidationException
	{
		ArrayList<IntegrationEvent> ret = new ArrayList<>();
		for (IntegrationEventEditorComponent wcomponent : events)
		{
			ret.add(wcomponent.getIntegrationEvent());
		}

		return ret;
	}
}
