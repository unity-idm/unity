/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.bus.RefreshEvent;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.shared.ui.Orientation;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Component showing a groups browser.
 * 
 * @author K. Benedyczak
 */
@SuppressWarnings("serial")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupBrowserComponent extends SafePanel
{
	private GroupsTree groupsTree;
	private UnityMessageSource msg;
	
	@Autowired
	public GroupBrowserComponent(GroupsTree groupsTree, UnityMessageSource msg)
	{
		this.groupsTree = groupsTree;
		this.msg = msg;
		init();
	}


	private void init()
	{
		setCaption(msg.getMessage("GroupBrowser.caption"));
		
		Toolbar toolbar = new Toolbar(groupsTree, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(groupsTree.getActionHandlers());
		ComponentWithToolbar treeWithToolbar = new ComponentWithToolbar(groupsTree, toolbar);
		treeWithToolbar.setWidth(100, Unit.PERCENTAGE);
		
		setContent(treeWithToolbar);
		setStyleName(Styles.vPanelLight.toString());
		setSizeFull();
		EventsBus bus = WebSession.getCurrent().getEventBus();
		bus.addListener(new EventListener<RefreshEvent>()
		{
			@Override
			public void handleEvent(RefreshEvent event)
			{
				groupsTree.refresh();
			}
		}, RefreshEvent.class);
	}
}




