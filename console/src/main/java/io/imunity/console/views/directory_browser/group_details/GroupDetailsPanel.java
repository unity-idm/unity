/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_details;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.console.views.directory_browser.RefreshAndSelectEvent;
import io.imunity.console.views.directory_browser.group_browser.GroupChangedEvent;
import io.imunity.vaadin.elements.ErrorLabel;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import static io.imunity.vaadin.elements.CssClassNames.MONOSPACE;
import static io.imunity.vaadin.elements.CssClassNames.SMALL_GAP;

@PrototypeComponent
public class GroupDetailsPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, GroupDetailsPanel.class);

	private final MessageSource msg;
	private final GroupsManagement groupsManagement;
	private final VerticalLayout main;
	private final TextField path;
	private final AttributeStatementsComponent attrStatements;

	private Group group;

	GroupDetailsPanel(MessageSource msg, AttributeStatementController controller, GroupsManagement groupsManagement)
	{
		this.msg = msg;
		this.groupsManagement = groupsManagement;

		main = new VerticalLayout();
		main.setClassName(SMALL_GAP.getName());
		main.setPadding(false);
		main.setSizeFull();

		attrStatements = new AttributeStatementsComponent(msg, controller);
		path = new TextField();
		path.setWidthFull();
		path.setReadOnly(true);
		path.addClassName(MONOSPACE.getName());
		setSizeFull();
		add(main);

		EventsBus eventBus = WebSession.getCurrent().getEventBus();
		eventBus.addListener(event -> refreshAndEnsureSelection(), RefreshAndSelectEvent.class);
		eventBus.addListener(event -> setGroup(event.group(), event.multi()), GroupChangedEvent.class);
		setGroup(null, false);
	}

	private void refreshAndEnsureSelection()
	{
		setGroup(group == null ? new Group("/") : group, false);
	}

	private void setGroup(Group group, boolean multi)
	{
		removeAll();
		main.removeAll();
		this.group = group;
		if (multi)
		{
			add(new HorizontalLayout(VaadinIcon.EXCLAMATION_CIRCLE_O.create(),
					new Span(msg.getMessage("GroupDetails.multiGroup"))));
			return;
		}
		if (group == null)
		{
			add(new HorizontalLayout(VaadinIcon.EXCLAMATION_CIRCLE_O.create(),
					new Span(msg.getMessage("GroupDetails.noGroup"))));
			return;
		}
		try
		{
			GroupContents contents = groupsManagement.getContents(group.getPathEncoded(), GroupContents.EVERYTHING);
			Group rGroup = contents.getGroup();
			main.add(new Html("<h5>" + msg.getMessage("GroupDetails.infoLabel", group.getDisplayedNameShort(msg).getValue(msg),
					String.valueOf(contents.getMembers().size()),
					String.valueOf(contents.getSubGroups().size())) + "</h5>"));
			main.add(path, attrStatements);
			attrStatements.setInput(rGroup);
			path.setValue(group.getPathEncoded());
			add(main);
		} catch (AuthorizationException e)
		{
			add(VaadinIcon.EXCLAMATION_CIRCLE_O.create(), new ErrorLabel(msg.getMessage("GroupDetails.noReadAuthz", group)));
		} catch (Exception e)
		{
			log.fatal("Problem retrieving group contents of " + group, e);
			add(VaadinIcon.EXCLAMATION_CIRCLE_O.create(), new ErrorLabel(msg.getMessage("GroupDetails.internalError", e.getMessage())));
		}
	}
}
