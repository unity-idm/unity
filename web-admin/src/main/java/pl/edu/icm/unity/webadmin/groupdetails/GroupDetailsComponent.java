/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Component providing Group details. The most complicated part is attribute statements handling which
 * is implemented in {@link AttributeStatementsTable}. This component shows generic data as chosen group and its description and
 * manages events and the overall layout.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupDetailsComponent extends Panel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, GroupDetailsComponent.class);
	private UnityMessageSource msg;
	private GroupsManagement groupsManagement;
	
	private Label info;
	private DescriptionTextArea description;
	private AttributeStatementsTable attrStatements;
	
	@Autowired
	public GroupDetailsComponent(UnityMessageSource msg, GroupsManagement groupsManagement, 
			AttributeHandlerRegistry attributeHandlersReg, AttributesManagement attrsMan)
	{
		this.msg = msg;
		this.groupsManagement = groupsManagement;

		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setSizeFull();
		main.setMargin(new MarginInfo(true, false, false, false));
	
		info = new Label();
		info.setVisible(false);
		info.setHeight(100, Unit.PERCENTAGE);

		description = new DescriptionTextArea(msg.getMessage("GroupDetails.description"), true, "");

		attrStatements = new AttributeStatementsTable(msg, groupsManagement, attributeHandlersReg,
				attrsMan);
		
		main.addComponents(info, description, attrStatements);
		main.setExpandRatio(info, 1.0f);
		main.setExpandRatio(attrStatements, 1.0f);
		
		setSizeFull();
		setContent(main);
		setStyleName(Reindeer.PANEL_LIGHT);

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
			setCaption(msg.getMessage("GroupDetails.captionNoGroup"));
			setProblem(msg.getMessage("GroupDetails.noGroup"));
			return;
		}
		setCaption(msg.getMessage("GroupDetails.caption", group));
		description.setVisible(true);
		attrStatements.setVisible(true);
		info.setVisible(false);
		
		try
		{
			GroupContents contents = groupsManagement.getContents(group, GroupContents.METADATA);
			description.setValue(contents.getGroup().getDescription());
			attrStatements.setInput(contents.getGroup());
		} catch (AuthorizationException e)
		{
			setProblem(msg.getMessage("GroupDetails.noReadAuthz", group));
		} catch (Exception e)
		{
			log.fatal("Problem retrieving group contents of " + group, e);
			setProblem(msg.getMessage("GroupDetails.internalError", e.toString()));
		}
	}
	
	private void setProblem(String message)
	{
		info.setValue(message);
		info.setVisible(true);
		description.setVisible(false);
		attrStatements.setVisible(false);
	}
}
