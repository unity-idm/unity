/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.groupdetails;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.directoryBrowser.RefreshAndSelectEvent;
import io.imunity.webconsole.directoryBrowser.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent.Level;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Component providing Group details. The most complicated part is attribute
 * statements handling which is implemented in {@link AttributeStatementsComponent}.
 * This component shows generic data as chosen group and its description and
 * manages events and the overall layout.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupDetailsPanel extends SafePanel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, GroupDetailsPanel.class);
	private MessageSource msg;
	private GroupsManagement groupsManagement;

	private VerticalLayout main;
	private TextField path;
	private AttributeStatementsComponent attrStatements;
	private Group group;

	@Autowired
	public GroupDetailsPanel(MessageSource msg, AttributeStatementController controller,
			GroupsManagement groupsManagement)
	{
		this.msg = msg;
		this.groupsManagement = groupsManagement;

		main = new VerticalLayout();
		main.setMargin(false);
		main.setSizeFull();

		attrStatements = new AttributeStatementsComponent(msg, controller);
		path = new TextField();
		path.setWidthFull();
		path.setReadOnly(true);
		
		main.addComponents(path, attrStatements);
		main.setExpandRatio(attrStatements, 1);

		setSizeFull();
		setContent(main);
		setStyleName(Styles.vPanelLight.toString());

		EventsBus eventBus = WebSession.getCurrent().getEventBus();
		eventBus.addListener(event -> refreshAndEnsureSelection(), RefreshAndSelectEvent.class);
		eventBus.addListener(event -> setGroup(event.getGroup()), GroupChangedEvent.class);
		setGroup(null);
	}

	private void refreshAndEnsureSelection()
	{
		setGroup(group == null ? new Group("/") : group);
	}

	private void setGroup(Group group)
	{
		this.group = group;
		if (group == null)
		{
			setProblem(msg.getMessage("GroupDetails.noGroup"), Level.warning);
			return;
		}
		try
		{
			GroupContents contents = groupsManagement.getContents(group.getPathEncoded(), GroupContents.EVERYTHING);
			Group rGroup = contents.getGroup();
			setCaptionFromBundle(msg, "GroupDetails.infoLabel", group.getDisplayedNameShort(msg).getValue(msg),
					String.valueOf(contents.getMembers().size()),
					String.valueOf(contents.getSubGroups().size()));
			attrStatements.setInput(rGroup);
			path.setValue(group.getPathEncoded());
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
