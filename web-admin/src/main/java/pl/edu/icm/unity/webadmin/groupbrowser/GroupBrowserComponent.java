/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.Orientation;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.bus.RefreshEvent;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar2;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar2;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

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
		
		Toolbar2<TreeNode> toolbar = new Toolbar2<>(Orientation.HORIZONTAL);
		groupsTree.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(groupsTree.getActionHandlers());
		ComponentWithToolbar2 treeWithToolbar = new ComponentWithToolbar2(groupsTree, toolbar);
		treeWithToolbar.setSizeFull();
		
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




