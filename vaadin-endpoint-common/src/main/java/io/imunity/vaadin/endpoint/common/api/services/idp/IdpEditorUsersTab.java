/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.idp;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializablePredicate;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.elements.grid.EditableGrid;
import io.imunity.vaadin.elements.grid.GridSearchFieldFactory;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorBase;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.*;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;

/**
 * Common Idp service editor users tab
 * 
 * @author P.Piernik
 *
 */
public class IdpEditorUsersTab extends VerticalLayout implements ServiceEditorBase.EditorTab
{
	protected MessageSource msg;
	protected Binder<?> configBinder;
	protected List<Group> allGroups;
	private final List<IdpUser> allUsers;
	private final List<String> allAttrTypes;
	private SerializablePredicate<IdpUser> searchFilter = null;
	protected Map<String, String> availableClients;
	private EditableGrid<ActiveValueConfig> releasedAttrsGrid;

	public IdpEditorUsersTab(MessageSource msg, List<Group> groups,
			List<IdpUser> allUsers, List<String> attrTypes)
	{
		this.msg = msg;
		this.allGroups = groups;
		this.allUsers = allUsers;
		this.allAttrTypes = attrTypes;
		availableClients = new HashMap<>();
	}

	public void initUI(Binder<?> configBinder)
	{
		this.configBinder = configBinder;
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setPadding(false);
		mainLayout.add(buildUsersSection());
		mainLayout.add(buildReleasedAttributesSection());
		add(mainLayout);
	}

	protected Component buildUsersSection()
	{
		VerticalLayout mainClientLayout = new VerticalLayout();
		mainClientLayout.setMargin(false);

		GridWithActionColumn<IdpUser> usersGrid = new GridWithActionColumn<>(msg::getMessage, Collections.emptyList());
		usersGrid.addColumn(u -> "[" + u.entity() + "] " + (u.name() != null ? u.name() : ""))
				.setHeader(msg.getMessage("IdpEditorUsersTab.entity"));
		usersGrid.addColumn(u -> u.state().toString())
				.setHeader(msg.getMessage("IdpEditorUsersTab.status"));
		usersGrid.setItems(allUsers);


		SearchField searchText = GridSearchFieldFactory.generateSearchField(usersGrid, msg::getMessage);
		searchText.addValueChangeListener(event -> {
			String searched = event.getValue();
			if (searchFilter != null)
			{
				usersGrid.removeFilter(searchFilter);
			}

			if (event.getValue() == null || event.getValue().isEmpty())
			{
				return;
			}
			searchFilter = e -> e.anyFieldContains(searched, msg::getMessage);
			usersGrid.addFilter(searchFilter);
		});

		Toolbar<IdpUser> toolbar = new Toolbar<>();
		toolbar.setWidthFull();
		toolbar.addSearch(searchText);
		ComponentWithToolbar usersGridWithToolbar = new ComponentWithToolbar(usersGrid, toolbar);
		usersGridWithToolbar.setSpacing(false);
		usersGridWithToolbar.setSizeFull();

		VerticalLayout usersWrapper = new VerticalLayout();
		usersWrapper.setMargin(true);
		usersWrapper.add(usersGridWithToolbar);

		FormLayout comboWrapper = new FormLayout();
		comboWrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		MandatoryGroupSelection groupCombo = new MandatoryGroupSelection(msg);
		groupCombo.setItems(allGroups);
		groupCombo.setRequiredIndicatorVisible(false);
		configBinder.forField(groupCombo)
				.bind("usersGroup");
		groupCombo.addValueChangeListener(e -> {
			usersGrid.clearFilters();
			usersGrid.addFilter(u -> u.group().equals(e.getValue().group().toString()));
			searchText.clear();
		});
		comboWrapper.addFormItem(groupCombo, msg.getMessage("IdpEditorUsersTab.usersGroup"));
		mainClientLayout.add(comboWrapper);
		mainClientLayout.add(usersWrapper);

		return mainClientLayout;
	}

	protected Component buildReleasedAttributesSection()
	{
		VerticalLayout mainAttrLayout = new VerticalLayout();
		mainAttrLayout.setPadding(false);

		releasedAttrsGrid = new EditableGrid<>(msg::getMessage, ActiveValueConfig::new);
		releasedAttrsGrid.addComboBoxColumn(s -> availableClients.get(s.getClientId()), ActiveValueConfig::setClientId, availableClients.values().stream().toList())
				.setHeader(msg.getMessage("IdpEditorUsersTab.client"))
				.setAutoWidth(true);
		releasedAttrsGrid.setWidth(TEXT_FIELD_BIG.value());

		MultiSelectComboBox<String> sattributes = new MultiSelectComboBox<>();
		sattributes.setItems(allAttrTypes);
		MultiSelectComboBox<String> mattributes = new MultiSelectComboBox<>();
		mattributes.setItems(allAttrTypes);

		releasedAttrsGrid.addCustomColumn(
				s -> new HashSet<>(s.getSingleSelectableAttributes()), (z, y) -> z.setSingleSelectableAttributes(List.of(y.toString())), sattributes)
				.setHeader(msg.getMessage("IdpEditorUsersTab.singleActiveValueSelection"));

		releasedAttrsGrid.addCustomColumn(
				s -> new HashSet<>(s.getMultiSelectableAttributes()), (z, y) -> z.setMultiSelectableAttributes(List.of(y.toString())), mattributes
				).setHeader(msg.getMessage("IdpEditorUsersTab.multipleActiveValueSelection"));

		configBinder.forField(releasedAttrsGrid).bind("activeValueSelections");

		mainAttrLayout.add(releasedAttrsGrid);

		AccordionPanel attrSection = new AccordionPanel(
				msg.getMessage("IdpEditorUsersTab.advancedAttributeReleaseControl"), mainAttrLayout);
		attrSection.setWidthFull();
		return attrSection;
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.FAMILY;
	}

	@Override
	public String getType()
	{
		return ServiceEditorComponent.ServiceEditorTab.USERS.toString();
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public String getCaption()
	{
		return null;
	}

	public void setAvailableClients(Map<String, String> clients)
	{
		this.availableClients = clients;

		List<ActiveValueConfig> remainingConfig = new ArrayList<>();
		for (ActiveValueConfig ac : releasedAttrsGrid.getValue())
		{
			if (clients.keySet().contains(ac.getClientId()))
			{
				remainingConfig.add(ac);
			}
		}

		releasedAttrsGrid.setValue(remainingConfig);
	}
}
