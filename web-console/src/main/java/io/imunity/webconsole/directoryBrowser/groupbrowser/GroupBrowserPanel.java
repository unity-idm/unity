/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.groupbrowser;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.bus.RefreshEvent;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Component showing a groups browser.
 * 
 * @author K. Benedyczak
 * 
 */
@PrototypeComponent
public class GroupBrowserPanel extends SafePanel
{
	private GroupsTreeGrid groupsTree;
	private UnityMessageSource msg;

	@Autowired
	public GroupBrowserPanel(GroupsTreeGrid groupsTree, UnityMessageSource msg)
	{
		this.groupsTree = groupsTree;
		this.msg = msg;
		init();
	}

	private void init()
	{
		setCaption(msg.getMessage("GroupBrowser.caption"));
		
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setSizeFull();
		ComponentWithToolbar treeWithToolbar = new ComponentWithToolbar(groupsTree, groupsTree.getToolbar(),
				Alignment.BOTTOM_LEFT);
		treeWithToolbar.setSizeFull();
		wrapper.setMargin(new MarginInfo(true, false));
		wrapper.setSpacing(true);
		wrapper.addComponent(treeWithToolbar);
		
		setContent(wrapper);
		setStyleName(Styles.vPanelLight.toString());
		setSizeFull();
		EventsBus bus = WebSession.getCurrent().getEventBus();
		bus.addListener(event -> groupsTree.refresh(), RefreshEvent.class);
	}
}
