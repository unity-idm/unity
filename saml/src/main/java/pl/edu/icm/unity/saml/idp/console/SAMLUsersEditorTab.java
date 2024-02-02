/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.elements.grid.EditableGrid;
import io.imunity.vaadin.endpoint.common.api.services.idp.IdpEditorUsersTab;
import io.imunity.vaadin.endpoint.common.api.services.idp.IdpUser;
import io.imunity.vaadin.endpoint.common.api.services.idp.MandatoryGroupSelection;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;

/**
 * SAML service editor users tab
 * 
 * @author P.Piernik
 *
 */
public class SAMLUsersEditorTab extends IdpEditorUsersTab
{
	private Select<String> clientsCombo;
	private EditableGrid<GroupMapping> groupMappings;

	public SAMLUsersEditorTab(MessageSource msg, List<Group> groups,
			List<IdpUser> allUsers, List<String> attrTypes)
	{
		super(msg, groups, allUsers, attrTypes);
	}

	public void initUI(Binder<?> configBinder)
	{
		this.configBinder = configBinder;

		setPadding(false);
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setPadding(false);
		mainLayout.add(buildUsersSection());
		mainLayout.add(buildGroupMappingsSection());
		mainLayout.add(buildReleasedAttributesSection());
		add(mainLayout);
	}

	protected Component buildGroupMappingsSection()
	{
		VerticalLayout groupMappingLayout = new VerticalLayout();
		groupMappingLayout.setMargin(false);

		clientsCombo = new Select<>();
		groupMappings = new EditableGrid<>(msg::getMessage, GroupMapping::new);
		groupMappingLayout.add(groupMappings);
		groupMappings.addCustomColumn(GroupMapping::getClientId, GroupMapping::setClientId, clientsCombo)
				.setHeader(msg.getMessage("IdpEditorUsersTab.client"))
				.setAutoWidth(true);

		MandatoryGroupSelection groupCombo = new MandatoryGroupSelection(msg);
		groupCombo.setWidth(TEXT_FIELD_BIG.value());
		groupCombo.setItems(allGroups);
		groupCombo.setRequiredIndicatorVisible(false);
		groupMappings.addCustomColumn(GroupMapping::getGroup, GroupMapping::setGroup,
				groupCombo).setHeader(msg.getMessage("SAMLUsersEditorTab.group"));
		configBinder.forField(groupMappings).bind("groupMappings");

		AccordionPanel accordionPanel = new AccordionPanel(
				msg.getMessage("SAMLUsersEditorTab.groupMapping"), groupMappingLayout);
		accordionPanel.setWidth(TEXT_FIELD_BIG.value());
		return accordionPanel;
	}

	@Override
	public void setAvailableClients(Map<String, String> clients)
	{
		super.setAvailableClients(clients);
		clientsCombo.setItems(clients.keySet());
		clientsCombo.setItemLabelGenerator(clients::get);

		List<GroupMapping> remainingConfig = new ArrayList<>();
		for (GroupMapping ac : groupMappings.getValue())
		{
			if (clients.keySet().contains(ac.getClientId()))
			{
				remainingConfig.add(ac);
			}
		}

		groupMappings.setValue(remainingConfig);
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.FAMILY;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("IdpServiceEditorBase.users");
	}
}
