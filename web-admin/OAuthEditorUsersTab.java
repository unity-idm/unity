/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.service;

import java.util.Collections;
import java.util.List;

import com.vaadin.data.Binder;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.reg.invitations.InvitationEntry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SearchField;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;

/**
 * 
 * @author P.Piernik
 *
 */
public class OAuthEditorUsersTab extends CustomComponent implements EditorTab
{
	private UnityMessageSource msg;
	private Binder<OAuthServiceConfiguration> configBinder;
	private List<Group> groups;
	private List<OAuthUser> allUsers;
	private SerializablePredicate<OAuthUser> searchFilter = null;

	public OAuthEditorUsersTab(UnityMessageSource msg, Binder<OAuthServiceConfiguration> configBinder,
			List<Group> groups, List<OAuthUser> allUsers)
	{
		this.msg = msg;
		this.configBinder = configBinder;
		this.groups = groups;
		this.allUsers = allUsers;
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("IdpServiceEditorBase.users"));
		setIcon(Images.family.getResource());

		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);
		mainLayout.addComponent(buildUsersSection());
		setCompositionRoot(mainLayout);
	}

	private Component buildUsersSection()
	{
		VerticalLayout mainClientLayout = new VerticalLayout();
		mainClientLayout.setMargin(false);

		GridWithActionColumn<OAuthUser> usersList = new GridWithActionColumn<>(msg, Collections.emptyList());
		usersList.setActionColumnHidden(true);
		usersList.addColumn(u -> u.name, msg.getMessage("OAuthEditorUsersTab.entity"), 20);
		usersList.addColumn(u -> u.identity, msg.getMessage("OAuthEditorUsersTab.identity"), 20);
		usersList.addColumn(u -> u.identityType, msg.getMessage("OAuthEditorUsersTab.identityType"), 20);
		usersList.addColumn(u -> u.state.toString(), msg.getMessage("OAuthEditorUsersTab.status"), 20);
		usersList.setItems(allUsers);
		usersList.setHeightByRows(true);	
		usersList.setHeightByRows(10);

		
		SearchField searchText = FilterableGridHelper.getRowSearchField(msg);
		searchText.addValueChangeListener(event -> {
			String searched = event.getValue();
			if (searchFilter != null)
			{
				usersList.removeFilter(searchFilter);
			}
			
			if (event.getValue() == null || event.getValue().isEmpty())
			{
				return;
			}
			searchFilter = e -> e.anyFieldContains(searched, msg);
			usersList.addFilter(searchFilter);
		});
		
		Toolbar<OAuthUser> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addSearch(searchText, Alignment.MIDDLE_RIGHT);
		ComponentWithToolbar usersGridWithToolbar = new ComponentWithToolbar(usersList, toolbar, Alignment.BOTTOM_LEFT);
		usersGridWithToolbar.setSpacing(false);
		usersGridWithToolbar.setSizeFull();
	
		VerticalLayout clientsWrapper = new VerticalLayout();
		clientsWrapper.setMargin(true);
		clientsWrapper.addComponent(usersGridWithToolbar);

		FormLayoutWithFixedCaptionWidth comboWrapper = new FormLayoutWithFixedCaptionWidth();
		MandatoryGroupSelection groupCombo = new MandatoryGroupSelection(msg);
		groupCombo.setWidth(30, Unit.EM);
		groupCombo.setCaption(msg.getMessage("OAuthEditorUsersTab.usersGroup"));
		groupCombo.setItems(groups);
		groupCombo.setRequiredIndicatorVisible(false);
		configBinder.forField(groupCombo).bind("usersGroup");
		groupCombo.addValueChangeListener(e -> {
			usersList.clearFilters();
			usersList.addFilter(u -> u.group.equals(e.getValue().group.toString()));
			searchText.clear();
		});
		comboWrapper.addComponent(groupCombo);

		mainClientLayout.addComponent(comboWrapper);
		mainClientLayout.addComponent(clientsWrapper);

		return mainClientLayout;
	}

	@Override
	public ServiceEditorTab getType()
	{
		return ServiceEditorTab.USERS;
	}

	@Override
	public CustomComponent getComponent()
	{
		return this;
	}

}
