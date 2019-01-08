/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webadmin.attributeclass.RequiredAttributesDialog;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webadmin.utils.GroupManagementHelper;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Entity creation dialog. Allows for choosing an initial identity, general entity settings.
 * The entity can be optionally added to a pre-selected group.
 * @author K. Benedyczak
 */
public class EntityCreationDialog extends IdentityCreationDialog
{
	private EntityManagement identitiesMan;
	private CredentialRequirementManagement credReqMan;
	private GroupManagementHelper groupHelper;
	private AttributeTypeManagement attrMan;
	
	private String initialGroup;
	private CheckBox addToGroup;
	private ComboBox<String> credentialRequirement;
	private EnumComboBox<EntityState> entityState;
	private Collection<AttributeType> allTypes;
	private EventsBus bus;
	
	public EntityCreationDialog(UnityMessageSource msg, String initialGroup, EntityManagement identitiesMan,
			CredentialRequirementManagement credReqMan, 
			AttributeTypeManagement attrMan,
			IdentityEditorRegistry identityEditorReg, 
			GroupManagementHelper groupHelper,
			Consumer<Identity> callback)
	{
		super(msg.getMessage("EntityCreation.caption"), msg, identitiesMan, identityEditorReg, callback);
		this.attrMan = attrMan;
		this.initialGroup = initialGroup;
		this.identitiesMan = identitiesMan;
		this.credReqMan = credReqMan;
		this.groupHelper = groupHelper;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	@Override
	protected FormLayout getContents() throws EngineException
	{
		FormLayout main = super.getContents();
	
		try
		{
			allTypes = attrMan.getAttributeTypes();
		} catch (EngineException e1)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.cantGetAttrTypes"));
			throw e1;
		}
		
		addToGroup = new CheckBox(msg.getMessage("EntityCreation.addToGroup", initialGroup));
		addToGroup.setValue(true);
		if (initialGroup.equals("/"))
			addToGroup.setEnabled(false);
		
		credentialRequirement = new ComboBox<>(msg.getMessage("EntityCreation.credReq"));
		Collection<CredentialRequirements> credReqs;
		try
		{
			credReqs = credReqMan.getCredentialRequirements();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.cantGetcredReq"));
			throw e;
		}
		if (credReqs.isEmpty())
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.credReqMissing"));
			throw new IllegalCredentialException(msg.getMessage("EntityCreation.credReqMissing"));
		}
		credentialRequirement.setItems(credReqs.stream().map(cr -> cr.getName()));
		credentialRequirement.setSelectedItem(credReqs.iterator().next().getName());
		credentialRequirement.setEmptySelectionAllowed(false);
		
		entityState = new EnumComboBox<EntityState>(msg.getMessage("EntityCreation.initialState"), msg, 
				"EntityState.", EntityState.class, EntityState.valid);
		
		main.addComponents(addToGroup, credentialRequirement, entityState);
		main.setSizeFull();
		return main;
	}

	@Override
	protected void onConfirm()
	{
		final IdentityParam toAdd;
		try
		{
			toAdd = identityEditor.getValue();
		} catch (IllegalIdentityValueException e)
		{
			return;
		}
		
		Set<String> required;
		try
		{
			required = groupHelper.getRequiredAttributes("/");
		} catch (EngineException e)
		{
			return;
		}
		
		if (required.isEmpty())
		{
			doCreate(toAdd, new ArrayList<>(0));
		} else
		{
			RequiredAttributesDialog attrDialog = new RequiredAttributesDialog(
					msg, msg.getMessage("EntityCreation.requiredAttributesInfo"), 
					required, groupHelper.getAttrHandlerRegistry(), allTypes, "/", 
					new RequiredAttributesDialog.Callback()
					{
						@Override
						public void onConfirm(List<Attribute> attributes)
						{
							doCreate(toAdd, attributes);
						}

						@Override
						public void onCancel()
						{
						}
					});
			attrDialog.show();
		}
		
	}
	
	private void doCreate(IdentityParam toAdd, List<Attribute> attributes)
	{
		Identity created;
		try
		{
			created = identitiesMan.addEntity(toAdd, (String)credentialRequirement.getValue(), 
					entityState.getValue(), extractAttributes.getValue(),
					attributes);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("EntityCreation.entityCreateError"), e);
			return;
		}
		
		if (addToGroup.getValue())
		{
			Deque<String> missing = Group.getMissingGroups(initialGroup, 
					Collections.singleton("/"));
			groupHelper.addToGroup(missing, created.getEntityId(), toGroup -> 
			{
				if (toGroup.equals(initialGroup))
					bus.fireEvent(new GroupChangedEvent(toGroup));
			});
		}
		callback.accept(created);
		close();
	}
}
