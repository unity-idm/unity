/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.VerticalLayout;

/**
 * Component showing a groups browser.
 * 
 * @author K. Benedyczak
 */
@SuppressWarnings("serial")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupBrowserComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private GroupsTree groupsTree;
	
	@Autowired
	public GroupBrowserComponent(UnityMessageSource msg, GroupsTree groupsTree)
	{
		this.msg = msg;
		this.groupsTree = groupsTree;
		init();
	}


	private void init()
	{
		addComponent(groupsTree);
	}
	
	public void refresh()
	{
		groupsTree.refresh();
	}
}




