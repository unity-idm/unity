/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_browser;

import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.console.views.directory_browser.RefreshAndSelectEvent;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import static io.imunity.vaadin.elements.CssClassNames.SMALL_GAP;

@PrototypeComponent
public class GroupBrowserPanel extends VerticalLayout
{
	private final GroupsTreeGrid groupsTree;
	private final MessageSource msg;

	GroupBrowserPanel(GroupsTreeGrid groupsTree, MessageSource msg)
	{
		this.groupsTree = groupsTree;
		this.msg = msg;
		init();
		setClassName(SMALL_GAP.getName());
	}

	private void init()
	{
		removeAll();
		add(new H5(msg.getMessage("GroupBrowser.caption")));
		
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setSizeFull();
		ComponentWithToolbar treeWithToolbar = new ComponentWithToolbar(groupsTree, groupsTree.getToolbar());
		treeWithToolbar.setClassName(SMALL_GAP.getName());
		treeWithToolbar.setSizeFull();
		wrapper.add(treeWithToolbar);

		add(wrapper);
		setSizeFull();
		EventsBus bus = WebSession.getCurrent().getEventBus();
		bus.addListener(event -> groupsTree.refreshAndEnsureSelection(), RefreshAndSelectEvent.class);
	}
}
