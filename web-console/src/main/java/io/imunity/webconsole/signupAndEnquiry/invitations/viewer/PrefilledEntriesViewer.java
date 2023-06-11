/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import java.util.stream.Collectors;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invite.FormPrefill;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

@PrototypeComponent
class PrefilledEntriesViewer extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PrefilledEntriesViewer.class);

	private final MessageSource msg;
	private final AttributeHandlerRegistryV8 attrHandlersRegistry;
	private final GroupsManagement groupMan;

	private ListOfElementsWithActions<PrefilledEntry<IdentityParam>> identities;
	private VerticalLayout attributes;
	private VerticalLayout groups;

	private SafePanel identitiesPanel;
	private SafePanel attributesPanel;
	private SafePanel groupsPanel;

	PrefilledEntriesViewer(MessageSource msg, AttributeHandlerRegistryV8 attrHandlersRegistry, GroupsManagement groupMan)
	{
		this.msg = msg;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.groupMan = groupMan;
		initUI();
	}

	private void initUI()
	{
		identities = new ListOfElementsWithActions<>();
		identitiesPanel = new SafePanel(msg.getMessage("InvitationViewer.identities"), identities);

		attributes = new VerticalLayout();
		attributes.setSpacing(true);
		attributes.addStyleName(Styles.tinySpacing.toString());

		attributes.setMargin(true);
		attributesPanel = new SafePanel(msg.getMessage("InvitationViewer.attributes"), attributes);

		groups = new VerticalLayout();
		groups.setSpacing(true);
		groups.addStyleName(Styles.tinySpacing.toString());

		groups.setMargin(true);

		groupsPanel = new SafePanel(msg.getMessage("InvitationViewer.groups"), groups);
		groupsPanel.setWidth(100, Unit.PERCENTAGE);
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponents(identitiesPanel, attributesPanel, groupsPanel);
		setCompositionRoot(main);
		main.setCaption("Registration");

	}

	void setInput(BaseForm form, FormPrefill formPrefillation)
	{
		identitiesPanel.setVisible(!formPrefillation.getIdentities().isEmpty());
		identities.clearContents();
		formPrefillation.getIdentities().values().forEach(e -> identities.addEntry(e));

		attributesPanel.setVisible(!formPrefillation.getAttributes().isEmpty());
		attributes.removeAllComponents();
		formPrefillation.getAttributes().values().forEach(entry ->
		{
			String attr = attrHandlersRegistry.getSimplifiedAttributeRepresentation(entry.getEntry());
			Label l = new Label("[" + entry.getMode().name() + "] " + attr);
			attributes.addComponent(l);
		});

		groupsPanel.setVisible(!formPrefillation.getGroupSelections().isEmpty()
				|| (!formPrefillation.getAllowedGroups().isEmpty() && !(formPrefillation.getAllowedGroups().values()
						.stream().map(p -> p.getSelectedGroups()).flatMap(List::stream).count() == 0)));
		groups.removeAllComponents();
		setGroups(form, formPrefillation);
		setVisible(attributesPanel.isVisible() || identitiesPanel.isVisible() || groupsPanel.isVisible());

	}

	private void setGroups(BaseForm form, FormPrefill invitation)
	{
		if (form == null)
		{
			return;
		}

		for (int i = 0; i < form.getGroupParams().size(); i++)
		{
			GroupRegistrationParam gp = form.getGroupParams().get(i);
			if (gp == null)
				continue;

			StringBuilder row = new StringBuilder("[" + gp.getGroupPath() + "]");
			PrefilledEntry<GroupSelection> selectedGroup = invitation.getGroupSelections().get(i);
			boolean add = false;
			if (selectedGroup != null && !selectedGroup.getEntry().getSelectedGroups().isEmpty())
			{
				row.append(" [" + selectedGroup.getMode().name() + "] ");
				row.append(selectedGroup.getEntry().getSelectedGroups().stream().map(g -> getGroupDisplayedName(g))
						.collect(Collectors.toList()));
				add = true;
			}

			GroupSelection allowed = invitation.getAllowedGroups().get(i);
			if (allowed != null && !allowed.getSelectedGroups().isEmpty())
			{
				row.append(" " + msg.getMessage("InvitationViewer.groupLimitedTo") + " ");
				row.append(allowed.getSelectedGroups().stream().map(g -> getGroupDisplayedName(g))
						.collect(Collectors.toList()));
				add = true;
			}

			if (add)
			{
				groups.addComponent(new Label(row.toString()));
			}
		}
	}

	private String getGroupDisplayedName(String path)
	{
		try
		{
			return groupMan.getContents(path, GroupContents.METADATA).getGroup().getDisplayedName().getValue(msg);
		} catch (Exception e)
		{
			log.warn("Can not get group displayed name for group " + path);
			return path;
		}
	}

}
