/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services.idp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.vaadin.data.Binder;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.GridWithEditorInDetails;
import pl.edu.icm.unity.webui.common.GridWithEditorInDetails.EmbeddedEditor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SearchField;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.chips.ChipsWithFreeText;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;

/**
 * OAuth service editor users tab
 * 
 * @author P.Piernik
 *
 */
public class IdpEditorUsersTab extends CustomComponent implements EditorTab
{
	protected UnityMessageSource msg;
	protected Binder<?> configBinder;
	protected List<Group> allGroups;
	private List<IdpUser> allUsers;
	private SerializablePredicate<IdpUser> searchFilter = null;
	private List<String> allAttrTypes;
	protected Map<String, String> availableClients;
	private GridWithEditorInDetails<ActiveValueConfig> releasedAttrsGrid;

	public IdpEditorUsersTab(UnityMessageSource msg, Binder<?> configBinder, List<Group> groups,
			List<IdpUser> allUsers, List<String> attrTypes)
	{
		this.msg = msg;
		this.configBinder = configBinder;
		this.allGroups = groups;
		this.allUsers = allUsers;
		this.allAttrTypes = attrTypes;
		initUI();
	}

	protected void initUI()
	{
		setCaption(msg.getMessage("IdpServiceEditorBase.users"));
		setIcon(Images.family.getResource());
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);
		mainLayout.addComponent(buildUsersSection());
		mainLayout.addComponent(buildReleasedAttributesSection());
		setCompositionRoot(mainLayout);
	}

	protected Component buildUsersSection()
	{
		VerticalLayout mainClientLayout = new VerticalLayout();
		mainClientLayout.setMargin(false);

		GridWithActionColumn<IdpUser> usersGrid = new GridWithActionColumn<>(msg, Collections.emptyList());
		usersGrid.setActionColumnHidden(true);
		usersGrid.addColumn(u -> u.name, msg.getMessage("IdpEditorUsersTab.entity"), 20);
		usersGrid.addColumn(u -> u.identity, msg.getMessage("IdpEditorUsersTab.identity"), 20);
		usersGrid.addColumn(u -> u.identityType, msg.getMessage("IdpEditorUsersTab.identityType"), 20);
		usersGrid.addColumn(u -> u.state.toString(), msg.getMessage("IdpEditorUsersTab.status"), 20);
		usersGrid.setItems(allUsers);
		usersGrid.setHeightByRows(true);
		usersGrid.setHeightByRows(10);

		SearchField searchText = FilterableGridHelper.getRowSearchField(msg);
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
			searchFilter = e -> e.anyFieldContains(searched, msg);
			usersGrid.addFilter(searchFilter);
		});

		Toolbar<IdpUser> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addSearch(searchText, Alignment.MIDDLE_RIGHT);
		ComponentWithToolbar usersGridWithToolbar = new ComponentWithToolbar(usersGrid, toolbar,
				Alignment.BOTTOM_LEFT);
		usersGridWithToolbar.setSpacing(false);
		usersGridWithToolbar.setSizeFull();

		VerticalLayout usersWrapper = new VerticalLayout();
		usersWrapper.setMargin(true);
		usersWrapper.addComponent(usersGridWithToolbar);

		FormLayoutWithFixedCaptionWidth comboWrapper = new FormLayoutWithFixedCaptionWidth();
		MandatoryGroupSelection groupCombo = new MandatoryGroupSelection(msg);
		groupCombo.setWidth(30, Unit.EM);
		groupCombo.setCaption(msg.getMessage("IdpEditorUsersTab.usersGroup"));
		groupCombo.setItems(allGroups);
		groupCombo.setRequiredIndicatorVisible(false);
		configBinder.forField(groupCombo).bind("usersGroup");
		groupCombo.addValueChangeListener(e -> {
			usersGrid.clearFilters();
			usersGrid.addFilter(u -> u.group.equals(e.getValue().group.toString()));
			searchText.clear();
		});
		comboWrapper.addComponent(groupCombo);
		mainClientLayout.addComponent(comboWrapper);
		mainClientLayout.addComponent(usersWrapper);

		return mainClientLayout;
	}

	protected Component buildReleasedAttributesSection()
	{
		VerticalLayout mainAttrLayout = new VerticalLayout();
		mainAttrLayout.setMargin(false);

		releasedAttrsGrid = new GridWithEditorInDetails<>(msg, ActiveValueConfig.class,
				() -> new ActiveValueConfigEditor(msg, allAttrTypes, availableClients), u -> false);
		releasedAttrsGrid.addTextColumn(s -> availableClients.get(s.getClientId()),
				msg.getMessage("IdpEditorUsersTab.client"), 10);
		releasedAttrsGrid.addTextColumn(
				s -> s.getSingleSelectableAttributes() != null
						? String.join(",", s.getSingleSelectableAttributes())
						: "",
				msg.getMessage("IdpEditorUsersTab.singleActiveValueSelection"), 10);
		releasedAttrsGrid.addTextColumn(
				s -> s.getMultiSelectableAttributes() != null
						? String.join(",", s.getMultiSelectableAttributes())
						: "",
				msg.getMessage("IdpEditorUsersTab.multipleActiveValueSelection"), 10);

		releasedAttrsGrid.setMinHeightByRow(7);
		configBinder.forField(releasedAttrsGrid).bind("activeValueSelections");

		mainAttrLayout.addComponent(releasedAttrsGrid);

		CollapsibleLayout attrSection = new CollapsibleLayout(
				msg.getMessage("IdpEditorUsersTab.advancedAttributeReleaseControl"), mainAttrLayout);
		attrSection.expand();
		return attrSection;
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

	public static class ActiveValueConfigEditor extends CustomComponent implements EmbeddedEditor<ActiveValueConfig>
	{
		private Binder<ActiveValueConfig> binder;

		public ActiveValueConfigEditor(UnityMessageSource msg, List<String> attrTypes,
				Map<String, String> clients)
		{
			binder = new Binder<>(ActiveValueConfig.class);
			ComboBox<String> clientId = new ComboBox<>();
			clientId.setCaption(msg.getMessage("IdpEditorUsersTab.client"));
			clientId.setItems(clients.keySet());
			clientId.setItemCaptionGenerator(i -> clients.get(i));
			binder.forField(clientId).asRequired().bind("clientId");
			ChipsWithFreeText sattributes = new ChipsWithFreeText(msg);
			sattributes.setCaption(msg.getMessage("IdpEditorUsersTab.singleActiveValueSelection") + ":");
			sattributes.setItems(attrTypes);
			binder.forField(sattributes).bind("singleSelectableAttributes");
			ChipsWithFreeText mattributes = new ChipsWithFreeText(msg);
			mattributes.setCaption(
					msg.getMessage("IdpEditorUsersTab.multipleActiveValueSelection") + ":");
			mattributes.setItems(attrTypes);
			binder.forField(mattributes).bind("multiSelectableAttributes");
			FormLayout main = new FormLayout();
			main.setMargin(false);
			main.addComponent(clientId);
			main.addComponent(sattributes);
			main.addComponent(mattributes);
			setCompositionRoot(main);
			setSizeFull();
		}

		@Override
		public ActiveValueConfig getValue() throws FormValidationException
		{
			if (binder.validate().hasErrors())
			{
				throw new FormValidationException();
			}

			return binder.getBean();
		}

		@Override
		public void setValue(ActiveValueConfig value)
		{

			binder.setBean(value);
		}
	}
}
