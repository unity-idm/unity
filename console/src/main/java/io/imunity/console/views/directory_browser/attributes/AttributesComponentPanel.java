/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.attributes;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.console.views.directory_browser.identities.EntityChangedEvent;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import static io.imunity.vaadin.elements.CssClassNames.SMALL_GAP;


@PrototypeComponent
public class AttributesComponentPanel extends VerticalLayout
{
	private final MessageSource msg;
	private final AttributesGrid main;

	public AttributesComponentPanel(MessageSource msg, AttributesGrid main)
	{
		this.msg = msg;
		this.main = main;

		setSizeFull();
		addClassName(SMALL_GAP.getName());

		EventsBus bus = WebSession.getCurrent().getEventBus();
		bus.addListener(event -> setInput(event.entity(), event.group(), event.multiSelected()), EntityChangedEvent.class);
		setInput(null, new Group("/"), false);
	}

	private void setInput(EntityWithLabel owner, Group group, boolean multiselect)
	{
		removeAll();
		if (owner == null)
		{
			add(new H5(msg.getMessage("Attribute.captionNoEntity")));
			if (multiselect)
				add(new HorizontalLayout(VaadinIcon.EXCLAMATION_CIRCLE_O.create(),
						new Span(msg.getMessage("Attribute.multiEntitiesSelected"))));
			else
				add(new HorizontalLayout(VaadinIcon.EXCLAMATION_CIRCLE_O.create(),
						new Span(msg.getMessage("Attribute.noEntitySelected"))));
			return;
		}
		add(new Html("<h5>" + msg.getMessage("Attribute.caption", owner,
				group.getDisplayedNameShort(msg).getValue(msg)) + "</h5>"));

		main.setInput(owner, group.getPathEncoded());
		add(main);
	}
}
