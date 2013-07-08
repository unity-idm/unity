/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webadmin.utils.GroupManagementUtils;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Entity creation dialog. Allows for choosing an initial identity, general entity settings.
 * The entity can be optionally added to a pre-selected group.
 * @author K. Benedyczak
 */
public class EntityCreationDialog extends IdentityCreationDialog
{
	private String initialGroup;
	private IdentitiesManagement identitiesMan;
	private GroupsManagement groupsMan;
	private AuthenticationManagement authnMan;
	
	private CheckBox addToGroup;
	private ComboBox credentialRequirement;
	private EnumComboBox<EntityState> entityState;
	
	public EntityCreationDialog(UnityMessageSource msg, String initialGroup, IdentitiesManagement identitiesMan,
			GroupsManagement groupsMan, AuthenticationManagement authnMan, 
			IdentityEditorRegistry identityEditorReg, Callback callback)
	{
		super(msg.getMessage("EntityCreation.caption"), msg, identitiesMan, identityEditorReg, callback);
		this.initialGroup = initialGroup;
		this.identitiesMan = identitiesMan;
		this.groupsMan = groupsMan;
		this.authnMan = authnMan;
	}

	@Override
	protected FormLayout getContents()
	{
		FormLayout main = super.getContents();
		
		addToGroup = new CheckBox(msg.getMessage("EntityCreation.addToGroup", initialGroup));
		addToGroup.setValue(true);
		if (initialGroup.equals("/"))
			addToGroup.setEnabled(false);
		
		credentialRequirement = new ComboBox(msg.getMessage("EntityCreation.credReq"));
		Collection<CredentialRequirements> credReqs;
		try
		{
			credReqs = authnMan.getCredentialRequirements();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.cantGetcredReq"));
			throw new IllegalStateException();
		}
		if (credReqs.isEmpty())
		{
			ErrorPopup.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.credReqMissing"));
			throw new IllegalStateException();
		}
		for (CredentialRequirements cr: credReqs)
		{
			credentialRequirement.addItem(cr.getName());
		}
		credentialRequirement.select(credReqs.iterator().next().getName());

		credentialRequirement.setNullSelectionAllowed(false);
		
		entityState = new EnumComboBox<EntityState>(msg.getMessage("EntityCreation.initialState"), msg, 
				"EntityState.", EntityState.class, EntityState.valid);
		
		main.addComponents(addToGroup, credentialRequirement, entityState);
		main.setSizeFull();
		return main;
	}

	@Override
	protected void onConfirm()
	{
		String value;
		try
		{
			value = identityEditor.getValue();
		} catch (IllegalIdentityValueException e)
		{
			return;
		}
		String type = (String) identityType.getValue();
		IdentityParam toAdd = new IdentityParam(type, value, true);
		Identity created;
		try
		{
			created = identitiesMan.addEntity(toAdd, (String)credentialRequirement.getValue(), 
					entityState.getSelectedValue(), extractAttributes.getValue());
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("EntityCreation.entityCreateError"), e);
			return;
		}
		
		if (addToGroup.getValue())
		{
			Deque<String> missing = GroupManagementUtils.getMissingGroups(initialGroup, 
					Collections.singleton("/"));
			GroupManagementUtils.addToGroup(missing, created.getEntityId(), msg, groupsMan);
		}
		callback.onCreated();
		close();
	}
}
