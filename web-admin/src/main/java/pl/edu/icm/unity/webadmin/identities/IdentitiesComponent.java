/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.bus.EventsBus;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Component wrapping {@link IdentitiesTable}. Allows to configure its mode, 
 * feeds it with data to be visualised etc.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IdentitiesComponent extends Panel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, IdentitiesComponent.class);
	private static final List<String> EMPTY_LIST = new ArrayList<String>(0);
	private UnityMessageSource msg;
	private GroupsManagement groupsManagement;
	private IdentitiesTable identitiesTable;
	private Label info;
	
	@Autowired
	public IdentitiesComponent(UnityMessageSource msg, GroupsManagement groupsManagement,
			IdentitiesTable identitiesTable)
	{
		this.msg = msg;
		this.groupsManagement = groupsManagement;
		this.identitiesTable = identitiesTable;

		VerticalLayout main = new VerticalLayout();
		final CheckBox mode = new CheckBox(msg.getMessage("Identities.mode"));
		mode.setImmediate(true);
		main.addComponent(mode);
		mode.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				IdentitiesComponent.this.identitiesTable.setMode(mode.getValue());
			}
		});

		main.addComponent(identitiesTable);
		main.setExpandRatio(identitiesTable, 1.0f);
		
		info = new Label();
		info.setVisible(false);
		main.addComponent(info);
		
		main.setSpacing(true);
		main.setSizeFull();
		
		setSizeFull();
		setContent(main);
		setStyleName(Reindeer.PANEL_LIGHT);
		setCaption(msg.getMessage("Identities.caption"));

		EventsBus bus = WebSession.getCurrent().getEventBus();
		bus.addListener(new EventListener<GroupChangedEvent>()
		{
			@Override
			public void handleEvent(GroupChangedEvent event)
			{
				setGroup(event.getGroup());
			}
		}, GroupChangedEvent.class);
		setGroup(null);
	}
	
	private void setGroup(String group)
	{
		if (group == null)
		{
			identitiesTable.setInput(null, EMPTY_LIST);
			setProblem(msg.getMessage("Identities.noGroupSelected"));
			return;
		}
		try
		{
			GroupContents contents = groupsManagement.getContents(group, GroupContents.MEMBERS);
			identitiesTable.setInput(group, contents.getMembers());
			identitiesTable.setVisible(true);
			info.setVisible(false);
		} catch (AuthorizationException e)
		{
			setProblem(msg.getMessage("Identities.noReadAuthz", group));
		} catch (EngineException e)
		{
			log.error("Problem retrieving group contents of " + group, e);
			setProblem(msg.getMessage("Identities.internalError", e.toString()));
		}
	}
	
	private void setProblem(String message)
	{
		identitiesTable.setVisible(false);
		info.setValue(message);
		info.setVisible(true);
	}
}
