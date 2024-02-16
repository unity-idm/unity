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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializablePredicate;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
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
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

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
	protected Select<String> availableClientsCombobox;
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
		setPadding(false);
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
		mainClientLayout.setPadding(false);

		GridWithActionColumn<IdpUser> usersGrid = new GridWithActionColumn<>(msg::getMessage, Collections.emptyList());
		usersGrid.addColumn(u -> "[" + u.entity() + "] " + (u.name() != null ? u.name() : ""))
				.setHeader(msg.getMessage("IdpEditorUsersTab.entity"));
		usersGrid.addColumn(u -> u.state().toString())
				.setHeader(msg.getMessage("IdpEditorUsersTab.status"));
		usersGrid.setItems(allUsers);
		usersGrid.removeActionColumn();

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
		toolbar.setJustifyContentMode(JustifyContentMode.END);
		ComponentWithToolbar usersGridWithToolbar = new ComponentWithToolbar(usersGrid, toolbar);
		usersGridWithToolbar.setSpacing(false);
		usersGridWithToolbar.setSizeFull();

		VerticalLayout usersWrapper = new VerticalLayout();
		usersWrapper.setPadding(false);
		usersWrapper.add(usersGridWithToolbar);

		FormLayout comboWrapper = new FormLayout();
		comboWrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		MandatoryGroupSelection groupCombo = new MandatoryGroupSelection(msg);
		groupCombo.setItems(allGroups);
		groupCombo.setRequiredIndicatorVisible(false);
		groupCombo.setWidth(TEXT_FIELD_BIG.value());
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

		availableClientsCombobox = new Select<>();
		availableClientsCombobox.setRequiredIndicatorVisible(true);
		availableClientsCombobox.setItemLabelGenerator(item -> availableClients.get(item));
		releasedAttrsGrid = new EditableGrid<>(msg::getMessage, ActiveValueConfig::new);
		releasedAttrsGrid.setWidthFull();

		List<String> sorted = allAttrTypes.stream().sorted().toList();
		MultiSelectComboBox<String> sattributes = new CustomValuesMultiSelectComboBox();
		sattributes.setItems(sorted);
		sattributes.setWidth(TEXT_FIELD_MEDIUM.value());
		sattributes.setPlaceholder(msg.getMessage("typeOrSelect"));
		sattributes.setAutoExpand(MultiSelectComboBox.AutoExpandMode.BOTH);
		MultiSelectComboBox<String> mattributes = new CustomValuesMultiSelectComboBox();
		mattributes.setPlaceholder(msg.getMessage("typeOrSelect"));
		mattributes.setWidth(TEXT_FIELD_MEDIUM.value());
		mattributes.setAutoExpand(MultiSelectComboBox.AutoExpandMode.BOTH);
		mattributes.setItems(sorted);

		releasedAttrsGrid.addCustomColumn(
				activeValueConfig -> availableClients.get(activeValueConfig.getClientId()),
						ActiveValueConfig::setClientId,
						availableClientsCombobox
				)
					.setHeader(msg.getMessage("IdpEditorUsersTab.client"));

		releasedAttrsGrid.addCustomColumn(
				s -> String.join(",", s.getSingleSelectableAttributes()),
				s -> new HashSet<>(s.getSingleSelectableAttributes()),
				(z, y) -> z.setSingleSelectableAttributes(y.stream().toList()),
				sattributes
		)
				.setHeader(msg.getMessage("IdpEditorUsersTab.singleActiveValueSelection"))
				.setAutoWidth(true)
				.setFlexGrow(2);

		releasedAttrsGrid.addCustomColumn(
				s -> String.join(",", s.getMultiSelectableAttributes()),
				s -> new HashSet<>(s.getMultiSelectableAttributes()),
				(z, y) -> z.setMultiSelectableAttributes(y.stream().toList()), mattributes
		)
				.setHeader(msg.getMessage("IdpEditorUsersTab.multipleActiveValueSelection"))
				.setAutoWidth(true)
				.setFlexGrow(2);

		releasedAttrsGrid.addValueChangeListener(e ->
		{
			if(releasedAttrsGrid.isEditorOpen())
				return;
			reloadAvailableClients(e.getValue());
		});
		releasedAttrsGrid.addEditorCloseListener(() -> reloadAvailableClients(releasedAttrsGrid.getValue()));
		releasedAttrsGrid.addEditorOpenListener(() -> releasedAttrsGrid.setAddingEnabled(false));


		configBinder.forField(releasedAttrsGrid).bind("activeValueSelections");

		mainAttrLayout.add(releasedAttrsGrid);

		AccordionPanel attrSection = new AccordionPanel(
				msg.getMessage("IdpEditorUsersTab.advancedAttributeReleaseControl"), mainAttrLayout);
		attrSection.setWidthFull();
		return attrSection;
	}

	private void reloadAvailableClients(List<ActiveValueConfig> currentValues)
	{
		HashSet<String> availableClientIds = new HashSet<>(availableClients.keySet());
		currentValues.stream().map(ActiveValueConfig::getClientId).toList().forEach(availableClientIds::remove);
		availableClientsCombobox.setItems(availableClientIds);
		releasedAttrsGrid.setAddingEnabled(!availableClientIds.isEmpty());
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
		return msg.getMessage("IdpServiceEditorBase.users");
	}

	public void setAvailableClients(Map<String, String> clients)
	{
		this.availableClients = clients;

		List<ActiveValueConfig> remainingConfig = new ArrayList<>();
		for (ActiveValueConfig ac : releasedAttrsGrid.getValue())
			if (clients.containsKey(ac.getClientId()))
				remainingConfig.add(ac);

		releasedAttrsGrid.setValue(remainingConfig);
		reloadAvailableClients(remainingConfig);
	}
}
