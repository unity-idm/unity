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
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webadmin.attrstmt.StatementHandlersRegistry;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent.Level;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Component providing Group details. The most complicated part is attribute statements handling which
 * is implemented in {@link AttributeStatementsTable}. This component shows generic data as chosen 
 * group and its description and manages events and the overall layout.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupDetailsComponent extends SafePanel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, GroupDetailsComponent.class);
	private UnityMessageSource msg;
	private GroupsManagement groupsManagement;
	
	private VerticalLayout main;
	private Label displayedName;
	private DescriptionTextArea description;
	private GroupAttributesClassesTable acPanel;
	private AttributeStatementsTable attrStatements;
	
	@Autowired
	public GroupDetailsComponent(UnityMessageSource msg, GroupsManagement groupsManagement, 
			AttributeHandlerRegistry attributeHandlersReg, AttributesManagement attrsMan,
			StatementHandlersRegistry statementHandlersReg)
	{
		this.msg = msg;
		this.groupsManagement = groupsManagement;

		main = new VerticalLayout();
		main.setSpacing(true);
		main.setSizeFull();
	
		FormLayout topLayout = new FormLayout();
		displayedName = new Label();
		displayedName.setCaption(msg.getMessage("displayedNameF"));
		description = new DescriptionTextArea(msg.getMessage("GroupDetails.description"), true, "");
		topLayout.addComponents(displayedName, description);
		
		acPanel = new GroupAttributesClassesTable(msg, groupsManagement, attrsMan);
		Toolbar acToolbar = new Toolbar(acPanel, Orientation.VERTICAL);
		acToolbar.addActionHandlers(acPanel.getHandlers());
		ComponentWithToolbar acWithToolbar = new ComponentWithToolbar(acPanel, acToolbar);
		acWithToolbar.setSizeFull();
		
		attrStatements = new AttributeStatementsTable(msg, groupsManagement, 
				attrsMan, statementHandlersReg);
		
		Toolbar asToolbar = new Toolbar(attrStatements, Orientation.VERTICAL);
		asToolbar.addActionHandlers(attrStatements.getActionHandlers());
		ComponentWithToolbar asWithToolbar = new ComponentWithToolbar(attrStatements, asToolbar);
		asWithToolbar.setSizeFull();
		
		main.addComponents(topLayout, acWithToolbar, asWithToolbar);
		main.setExpandRatio(acWithToolbar, 0.5f);
		main.setExpandRatio(asWithToolbar, 0.5f);
		
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
			setProblem(msg.getMessage("GroupDetails.noGroup"), Level.warning);
			return;
		}
		setCaption(msg.getMessage("GroupDetails.caption", group));
		
		try
		{
			GroupContents contents = groupsManagement.getContents(group, GroupContents.METADATA);
			Group rGroup = contents.getGroup();
			displayedName.setValue(rGroup.getDisplayedName().getValue(msg));
			String desc = rGroup.getDescription().getValue(msg);
			description.setValue(desc == null ? "" : desc);
			attrStatements.setInput(rGroup);
			acPanel.setInput(rGroup);
			setContent(main);
		} catch (AuthorizationException e)
		{
			setProblem(msg.getMessage("GroupDetails.noReadAuthz", group), Level.error);
		} catch (Exception e)
		{
			log.fatal("Problem retrieving group contents of " + group, e);
			setProblem(msg.getMessage("GroupDetails.internalError", e.toString()), Level.error);
		}
	}
	
	private void setProblem(String message, Level level)
	{
		ErrorComponent errorC = new ErrorComponent();
		errorC.setMessage(message, level);
		setContent(errorC);
	}
}
