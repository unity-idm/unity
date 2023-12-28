/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.viewer;

import java.util.stream.Collectors;
import java.util.List;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.elements.Panel;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.ListOfElementsWithActions;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;


@PrototypeComponent
class PrefilledEntriesViewer extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PrefilledEntriesViewer.class);

	private final MessageSource msg;
	private final AttributeHandlerRegistry attrHandlersRegistry;
	private final GroupsManagement groupMan;

	private ListOfElementsWithActions<PrefilledEntry<IdentityParam>> identities;
	private VerticalLayout attributes;
	private VerticalLayout groups;

	private Panel identitiesPanel;
	private Panel attributesPanel;
	private Panel groupsPanel;
	
	private Consumer<Boolean> visibleChangeListener;

	PrefilledEntriesViewer(MessageSource msg, AttributeHandlerRegistry attrHandlersRegistry, GroupsManagement groupMan)
	{
		this.msg = msg;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.groupMan = groupMan;
		initUI();
	}

	private void initUI()
	{
		identities = new ListOfElementsWithActions<>(t -> new NativeLabel(t.toString()));
		identitiesPanel = new Panel(msg.getMessage("InvitationViewer.identities"));
		identitiesPanel.setWidthFull();
		identitiesPanel.setMargin(false);
		identitiesPanel.add(new VerticalLayout(identities));
		add(identitiesPanel);
		
		attributes = new VerticalLayout();
		attributes.setMargin(false);
		attributes.setSpacing(false);
		attributesPanel = new Panel(msg.getMessage("InvitationViewer.attributes"));
		attributesPanel.add(attributes);
		attributesPanel.setWidthFull();
		attributesPanel.setMargin(false);
		add(attributesPanel);

		groups = new VerticalLayout();
		groups.setMargin(false);
		groups.setSpacing(false);
		groupsPanel = new Panel(msg.getMessage("InvitationViewer.groups"));
		groupsPanel.add(groups);
		groupsPanel.setWidthFull();
		groupsPanel.setMargin(false);

		add(groupsPanel);
	
		setMargin(false);
		setPadding(false);
		setSizeFull();

	}

	void setInput(BaseForm form, FormPrefill formPrefillation)
	{
		identitiesPanel.setVisible(!formPrefillation.getIdentities().isEmpty());
		identities.clearContents();
		formPrefillation.getIdentities().values().forEach(e -> identities.addEntry(e));

		attributesPanel.setVisible(!formPrefillation.getAttributes().isEmpty());
		attributes.removeAll();
		formPrefillation.getAttributes().values().forEach(entry ->
		{
			String attr = attrHandlersRegistry.getSimplifiedAttributeRepresentation(entry.getEntry());
			NativeLabel l = new NativeLabel("[" + entry.getMode().name() + "] " + attr);
			attributes.add(l);
		});

		groupsPanel.setVisible(!formPrefillation.getGroupSelections().isEmpty()
				|| (!formPrefillation.getAllowedGroups().isEmpty() && !(formPrefillation.getAllowedGroups().values()
						.stream().map(p -> p.getSelectedGroups()).flatMap(List::stream).count() == 0)));
		groups.removeAll();
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
				groups.add(new NativeLabel(row.toString()));
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
	
	void addVisibleChangeListener(Consumer<Boolean> visibleChangeListener)
	{
		this.visibleChangeListener = visibleChangeListener;
	}
	
	@Override
	public void setVisible(boolean visible)
	{
		if(visibleChangeListener != null)
			visibleChangeListener.accept(visible);
		super.setVisible(visible);
	}

}
