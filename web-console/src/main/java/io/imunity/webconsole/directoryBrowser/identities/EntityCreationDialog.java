/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.directoryBrowser.groupbrowser.GroupChangedEvent;
import io.imunity.webconsole.directoryBrowser.groupbrowser.GroupManagementHelper;
import io.imunity.webconsole.directoryBrowser.identities.NewEntityCredentialsPanel.CredentialsPanelFactory;
import io.imunity.webconsole.directoryBrowser.identities.SingleCredentialPanel.ObtainedCredential;
import io.imunity.webconsole.directorySetup.attributeClasses.RequiredAttributesDialog;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmationEditMode;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.FormValidationRTException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistryV8;
import pl.edu.icm.unity.webui.common.widgets.InfoLabel;

/**
 * Entity creation dialog. Allows for choosing an initial identity, general entity settings.
 * The entity can be optionally added to a pre-selected group.
 * 
 * @author K. Benedyczak
 */
class EntityCreationDialog extends IdentityCreationDialog
{
	private EntityManagement identitiesMan;
	private CredentialRequirementManagement credReqMan;
	private GroupManagementHelper groupHelper;
	private AttributeTypeManagement attrMan;
	private final AttributeSupport attributeSupport;
	private final AttributeHandlerRegistryV8 reg;
	private Group initialGroup;
	private CheckBox addToGroup;
	private ComboBox<String> credentialRequirement;
	private EnumComboBox<EntityState> entityState;
	private Collection<AttributeType> allTypes;
	private EventsBus bus;
	private Collection<FixedAttributeEditor> attributeEditors;
	private CredentialsPanelFactory credentialsPanelFactory;
	private NewEntityCredentialsPanel newEntityCredentialsPanel;
	private TabSheet tabs;
	private EntityCredentialManagement ecredMan;
	
	EntityCreationDialog(MessageSource msg, Group initialGroup, EntityManagement identitiesMan,
			CredentialRequirementManagement credReqMan, 
			AttributeTypeManagement attrMan,
			IdentityEditorRegistryV8 identityEditorReg,
			GroupManagementHelper groupHelper,
			Consumer<Identity> callback,
			AttributeSupport attributeSupport,
			AttributeHandlerRegistryV8 reg,
			NewEntityCredentialsPanel.CredentialsPanelFactory credentialsPanelFactory,
			EntityCredentialManagement ecredMan)
	{
		super(msg.getMessage("EntityCreation.caption"), msg, identitiesMan, identityEditorReg, callback);
		this.attrMan = attrMan;
		this.initialGroup = initialGroup;
		this.identitiesMan = identitiesMan;
		this.credReqMan = credReqMan;
		this.groupHelper = groupHelper;
		this.attributeSupport = attributeSupport;
		this.reg = reg;
		this.credentialsPanelFactory = credentialsPanelFactory;
		this.ecredMan = ecredMan;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	@Override
	protected AbstractOrderedLayout getContents() throws EngineException
	{
		AbstractOrderedLayout main = super.getContents();
	
		try
		{
			allTypes = attrMan.getAttributeTypes();
		} catch (EngineException e1)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.cantGetAttrTypes"));
			throw e1;
		}

		main.addComponentAsFirst(new InfoLabel(msg.getMessage("EntityCreation.initialInfo")));
		
		tabs = new TabSheet();
		
		Tab attributesTab = tabs.addTab(buildAttributesTab());
		attributesTab.setCaption(msg.getMessage("EntityCreation.attributesTab"));

		Tab credentialsTab = tabs.addTab(buildCredentialsTab());
		credentialsTab.setCaption(msg.getMessage("EntityCreation.credentialsTab"));

		Tab advancedTab = tabs.addTab(buildAdvancedTab());
		advancedTab.setCaption(msg.getMessage("EntityCreation.advancedTab"));
		
		main.addComponent(tabs);
		main.setSizeFull();
		setSizeEm(50, 50);
		return main;
	}

	private Component buildAttributesTab() throws EngineException
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(new MarginInfo(true, false));
		attributeEditors = new ArrayList<>();
		getDesignatedAttributeUI(EntityNameMetadataProvider.NAME)
			.ifPresent(editor -> attributeEditors.add(editor));
		getDesignatedAttributeUI(ContactEmailMetadataProvider.NAME)
			.ifPresent(editor -> attributeEditors.add(editor));
		for (FixedAttributeEditor editor: attributeEditors)
		{
			FormLayout layout = FormLayoutWithFixedCaptionWidth.withShortCaptions();
			layout.setMargin(false);
			editor.placeOnLayout(layout);
			wrapper.addComponent(layout);
		}
		return wrapper;
	}

	private Optional<FixedAttributeEditor> getDesignatedAttributeUI(String metadataId) throws EngineException
	{
		List<AttributeType> designatedAttrTypeList = attributeSupport.getAttributeTypeWithMetadata(metadataId);
		if (!designatedAttrTypeList.isEmpty())
		{
			AttributeType designatedAttrType = designatedAttrTypeList.get(0);
			FixedAttributeEditor editor = new FixedAttributeEditor(msg, reg, AttributeEditContext.builder()
					.withAttributeGroup("/")
					.withAttributeType(designatedAttrType)
					.withRequired(false)
					.withConfirmationMode(ConfirmationEditMode.ADMIN)
					.build(), 
					false, 
					designatedAttrType.getDisplayedName().getValue(msg) + ":", 
					null);
			return Optional.of(editor);
		}
		return Optional.empty();
	}

	private List<Attribute> getPresetAttributes() throws FormValidationException
	{
		List<Attribute> ret = new ArrayList<>();
		for (FixedAttributeEditor editor : attributeEditors)
			editor.getAttribute().ifPresent(a -> ret.add(a));
		return ret;
	}

	private Component buildCredentialsTab() throws EngineException
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		FormLayout formLayout = FormLayoutWithFixedCaptionWidth.withShortCaptions();
		formLayout.setMargin(new MarginInfo(true, false));

		credentialRequirement = new ComboBox<>(msg.getMessage("EntityCreation.credReq"));
		Collection<CredentialRequirements> credReqs = credReqMan.getCredentialRequirements();
		credentialRequirement.setItems(credReqs.stream().map(cr -> cr.getName()));
		credentialRequirement.setSelectedItem(credReqs.iterator().next().getName());
		credentialRequirement.setEmptySelectionAllowed(false);
		
		formLayout.addComponent(credentialRequirement);
		main.addComponent(formLayout);
		
		newEntityCredentialsPanel = credentialsPanelFactory.getInstance(credentialRequirement.getValue());
		main.addComponent(newEntityCredentialsPanel);
		
		credentialRequirement.addValueChangeListener(event -> 
		{
			newEntityCredentialsPanel = credentialsPanelFactory.getInstance(credentialRequirement.getValue());
			main.replaceComponent(main.getComponent(main.getComponentCount()-1), newEntityCredentialsPanel);
		});
		return main;
	}
	
	private Component buildAdvancedTab() throws EngineException
	{
		FormLayout layout = FormLayoutWithFixedCaptionWidth.withShortCaptions();
		layout.setMargin(new MarginInfo(true, false));

		addToGroup = new CheckBox(msg.getMessage("EntityCreation.addToGroup", initialGroup));
		addToGroup.setValue(true);
		if (initialGroup.isTopLevel())
			addToGroup.setVisible(false);
		
		entityState = new EnumComboBox<EntityState>(msg.getMessage("EntityCreation.initialState"), msg, 
				"EntityState.", EntityState.class, EntityState.valid);

		layout.addComponents(addToGroup, entityState);
		return layout;
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

		try
		{
			newEntityCredentialsPanel.getCredentials();
		} catch (FormValidationRTException e)
		{
			tabs.setSelectedTab(1);
			NotificationPopup.showFormError(msg, msg.getMessage("EntityCreation.invalidCredential"));
			return;
		}
		
		Set<String> requiredInRoot;
		try
		{
			requiredInRoot = groupHelper.getRequiredAttributes("/");
		} catch (EngineException e)
		{
			return;
		}
		
		List<Attribute> attrsToAddInRoot;
		try
		{
			attrsToAddInRoot = getPresetAttributes();
		} catch (FormValidationException e)
		{
			return;
		}
		for (Attribute a: attrsToAddInRoot)
			requiredInRoot.remove(a.getName());
		
		if (requiredInRoot.isEmpty())
		{
			doCreate(toAdd, attrsToAddInRoot);
		} else
		{
			RequiredAttributesDialog attrDialog = new RequiredAttributesDialog(
					msg, msg.getMessage("EntityCreation.requiredAttributesInfo"), 
					requiredInRoot, groupHelper.getAttrHandlerRegistry(), allTypes, "/", 
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
					entityState.getValue(), attributes);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("EntityCreation.entityCreateError"), e);
			return;
		}
		
		if (addToGroup.getValue())
		{
			Deque<String> missing = Group.getMissingGroups(initialGroup.getPathEncoded(), 
					Collections.singleton("/"));
			groupHelper.addToGroup(missing, created.getEntityId(), toGroup -> 
			{
				if (toGroup.equals(initialGroup.getPathEncoded()))
					bus.fireEvent(new GroupChangedEvent(initialGroup));
			});
		}
		setupCredentials(created);
		callback.accept(created);
		close();
	}
	
	private void setupCredentials(Identity identity)
	{
		List<ObtainedCredential> credentials = newEntityCredentialsPanel.getCredentials();
		EntityParam entityP = new EntityParam(identity);
		for (ObtainedCredential credential: credentials)
			setupCredential(entityP, credential);
	}
	
	private void setupCredential(EntityParam entityP, ObtainedCredential credential)
	{
		try
		{
			ecredMan.setEntityCredential(entityP, credential.credentialId, credential.secrets);
			if (credential.setAsInvalid)
				ecredMan.setEntityCredentialStatus(entityP, credential.credentialId, 
						LocalCredentialState.outdated);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"CredentialChangeDialog.credentialUpdateError"), e);
		}
	}

}
