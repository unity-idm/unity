/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.GridWithEditor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;
import pl.edu.icm.unity.webui.console.services.idp.IdpEditorUsersTab;
import pl.edu.icm.unity.webui.console.services.idp.IdpUser;

/**
 * SAML service editor users tab
 * 
 * @author P.Piernik
 *
 */
public class SAMLUsersEditorTab extends IdpEditorUsersTab
{
	private ComboBox<String> clientsCombo;
	private GridWithEditor<GroupMapping> groupMappings;

	public SAMLUsersEditorTab(MessageSource msg, List<Group> groups,
			List<IdpUser> allUsers, List<String> attrTypes)
	{
		super(msg, groups, allUsers, attrTypes);
	}

	public void initUI(Binder<?> configBinder)
	{
		this.configBinder = configBinder;
		setCaption(msg.getMessage("IdpServiceEditorBase.users"));
		setIcon(Images.family.getResource());

		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);
		mainLayout.addComponent(buildUsersSection());
		mainLayout.addComponent(buildGroupMappingsSection());
		mainLayout.addComponent(buildReleasedAttributesSection());
		setCompositionRoot(mainLayout);
	}

	protected Component buildGroupMappingsSection()
	{
		VerticalLayout groupMappingLayout = new VerticalLayout();
		groupMappingLayout.setMargin(false);

		groupMappings = new GridWithEditor<>(msg, GroupMapping.class);
		groupMappingLayout.addComponent(groupMappings);
		clientsCombo = new ComboBox<String>();
		clientsCombo.setEmptySelectionAllowed(false);
		groupMappings.addCustomColumn(m -> m.getClientId(), m -> m, (t, v) -> t.setClientId(v), clientsCombo,
				msg.getMessage("IdpEditorUsersTab.client"), 30);
		MandatoryGroupSelection groupCombo = new MandatoryGroupSelection(msg);
		groupCombo.setWidth(30, Unit.EM);
		groupCombo.setItems(allGroups);
		groupCombo.setRequiredIndicatorVisible(false);
		groupMappings.addCustomColumn(m -> m.getGroup(),
				m -> m != null ? m.group.getDisplayedName().getValue(msg) : "", (t, v) -> t.setGroup(v),
				groupCombo, msg.getMessage("SAMLUsersEditorTab.group"), 30);
		configBinder.forField(groupMappings).bind("groupMappings");

		CollapsibleLayout groupMappingSection = new CollapsibleLayout(
				msg.getMessage("SAMLUsersEditorTab.groupMapping"), groupMappingLayout);
		groupMappingSection.collapse();
		return groupMappingSection;
	}

	@Override
	public void setAvailableClients(Map<String, String> clients)
	{
		super.setAvailableClients(clients);
		clientsCombo.setItems(clients.keySet());
		clientsCombo.setItemCaptionGenerator(c -> clients.get(c));

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
}
