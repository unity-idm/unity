/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import io.imunity.console.views.directory_browser.group_browser.GroupChangedEvent;
import io.imunity.console.views.directory_browser.group_browser.GroupManagementHelper;
import io.imunity.console.views.directory_setup.attribute_classes.RequiredAttributesDialog;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import io.imunity.vaadin.endpoint.common.plugins.attributes.*;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.*;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

class EntityCreationDialog extends IdentityCreationDialog
{
	private final EntityManagement identitiesMan;
	private final CredentialRequirementManagement credReqMan;
	private final GroupManagementHelper groupHelper;
	private final AttributeTypeManagement attrMan;
	private final AttributeSupport attributeSupport;
	private final AttributeHandlerRegistry reg;
	private final Group initialGroup;
	private final EventsBus bus;
	private final NewEntityCredentialsPanel.CredentialsPanelFactory credentialsPanelFactory;
	private final EntityCredentialManagement ecredMan;
	private Checkbox addToGroup;
	private ComboBox<String> credentialRequirement;
	private ComboBox<EntityState> entityState;
	private Collection<AttributeType> allTypes;
	private Collection<FixedAttributeEditor> attributeEditors;
	private NewEntityCredentialsPanel newEntityCredentialsPanel;
	private TabSheet tabs;

	EntityCreationDialog(MessageSource msg, Group initialGroup, EntityManagement identitiesMan,
			CredentialRequirementManagement credReqMan, 
			AttributeTypeManagement attrMan,
			IdentityEditorRegistry identityEditorReg,
			GroupManagementHelper groupHelper,
			Consumer<Identity> callback,
			AttributeSupport attributeSupport,
			AttributeHandlerRegistry reg,
			NewEntityCredentialsPanel.CredentialsPanelFactory credentialsPanelFactory,
			EntityCredentialManagement ecredMan,
			NotificationPresenter notificationPresenter)
	{
		super(msg.getMessage("EntityCreation.caption"), msg, identitiesMan, identityEditorReg, callback, notificationPresenter);
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
		addTitle(msg);
		add(getContents());
	}

	private void addTitle(MessageSource msg)
	{
		Span title = new Span(" " + msg.getMessage("EntityCreation.initialInfo"));
		title.addComponentAsFirst(VaadinIcon.EXCLAMATION_CIRCLE_O.create());
		addComponentAsFirst(title);
	}

	private VerticalLayout getContents()
	{
		VerticalLayout main = new VerticalLayout();
		try
		{
			allTypes = attrMan.getAttributeTypes();
		} catch (EngineException e1)
		{
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.cantGetAttrTypes"));
		}
		
		tabs = new TabSheet();
		tabs.setWidthFull();
		tabs.add(msg.getMessage("EntityCreation.attributesTab"), buildAttributesTab());
		tabs.add(msg.getMessage("EntityCreation.credentialsTab"), buildCredentialsTab());
		tabs.add(msg.getMessage("EntityCreation.advancedTab"), buildAdvancedTab());

		main.add(tabs);
		main.setPadding(false);
		setWidth("50em");
		setHeight("50em");
		return main;
	}

	private Component buildAttributesTab()
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setPadding(false);
		attributeEditors = new ArrayList<>();
		getDesignatedAttributeUI(EntityNameMetadataProvider.NAME)
			.ifPresent(editor -> attributeEditors.add(editor));
		getDesignatedAttributeUI(ContactEmailMetadataProvider.NAME)
			.ifPresent(editor -> attributeEditors.add(editor));
		for (FixedAttributeEditor editor: attributeEditors)
		{
			FormLayout layout = new FormLayout();
			layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
			editor.placeOnLayout(layout);
			wrapper.add(layout);
		}
		return wrapper;
	}

	private Optional<FixedAttributeEditor> getDesignatedAttributeUI(String metadataId)
	{
		List<AttributeType> designatedAttrTypeList;
		try
		{
			designatedAttrTypeList = attributeSupport.getAttributeTypeWithMetadata(metadataId);
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			throw new RuntimeException(e);
		}
		if (!designatedAttrTypeList.isEmpty())
		{
			AttributeType designatedAttrType = designatedAttrTypeList.get(0);
			FixedAttributeEditor editor = new FixedAttributeEditor(msg, reg, AttributeEditContext.builder()
					.withAttributeGroup("/")
					.withAttributeType(designatedAttrType)
					.withRequired(false)
					.withConfirmationMode(ConfirmationEditMode.ADMIN)
					.build(),
					new LabelContext(designatedAttrType.getDisplayedName().getValue(msg) + ":"),
					null);
			return Optional.of(editor);
		}
		return Optional.empty();
	}

	private List<Attribute> getPresetAttributes() throws FormValidationException
	{
		List<Attribute> ret = new ArrayList<>();
		for (FixedAttributeEditor editor : attributeEditors)
			editor.getAttribute().ifPresent(ret::add);
		return ret;
	}

	private Component buildCredentialsTab()
	{
		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		FormLayout formLayout = new FormLayout();
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		credentialRequirement = new ComboBox<>();
		Collection<CredentialRequirements> credReqs = null;
		try
		{
			credReqs = credReqMan.getCredentialRequirements();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			return main;
		}
		credentialRequirement.setItems(credReqs.stream().map(DescribedObjectROImpl::getName).toList());
		credentialRequirement.setValue(credReqs.iterator().next().getName());

		formLayout.addFormItem(credentialRequirement, msg.getMessage("EntityCreation.credReq"));
		main.add(formLayout);
		
		newEntityCredentialsPanel = credentialsPanelFactory.getInstance(credentialRequirement.getValue());
		main.add(newEntityCredentialsPanel);
		
		credentialRequirement.addValueChangeListener(event -> 
		{
			newEntityCredentialsPanel = credentialsPanelFactory.getInstance(credentialRequirement.getValue());
			main.replace(main.getComponentAt(main.getComponentCount()-1), newEntityCredentialsPanel);
		});
		return main;
	}
	
	private Component buildAdvancedTab()
	{
		FormLayout layout = new FormLayout();
		layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		addToGroup = new Checkbox(msg.getMessage("EntityCreation.addToGroup", initialGroup));
		addToGroup.setValue(true);

		entityState = new ComboBox<>();
		entityState.setItems(EntityState.values());
		entityState.setItemLabelGenerator(item -> msg.getMessage("EntityState." + item));
		entityState.setValue(EntityState.valid);
		entityState.setWidth(TEXT_FIELD_MEDIUM.value());

		FormLayout.FormItem formItem = layout.addFormItem(addToGroup, "");
		if (initialGroup.isTopLevel())
			formItem.setVisible(false);
		layout.addFormItem(entityState, msg.getMessage("EntityCreation.initialState"));
		return layout;
	}
	
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
			tabs.setSelectedIndex(1);
			notificationPresenter.showError(msg.getMessage("EntityCreation.invalidCredential"), e.getCause().getMessage());
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
					}, notificationPresenter);
			attrDialog.open();
		}
		
	}
	
	private void doCreate(IdentityParam toAdd, List<Attribute> attributes)
	{
		Identity created;
		try
		{
			created = identitiesMan.addEntity(toAdd, credentialRequirement.getValue(),
					entityState.getValue(), attributes);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("EntityCreation.entityCreateError"), e.getMessage());
			return;
		}
		
		if (addToGroup.getValue())
		{
			Deque<String> missing = Group.getMissingGroups(initialGroup.getPathEncoded(), 
					Collections.singleton("/"));
			groupHelper.addToGroup(missing, created.getEntityId(), toGroup -> 
			{
				if (toGroup.equals(initialGroup.getPathEncoded()))
					bus.fireEvent(new GroupChangedEvent(initialGroup, false));
			});
		}
		setupCredentials(created);
		callback.accept(created);
		close();
	}
	
	private void setupCredentials(Identity identity)
	{
		List<SingleCredentialPanel.ObtainedCredential> credentials = newEntityCredentialsPanel.getCredentials();
		EntityParam entityP = new EntityParam(identity);
		for (SingleCredentialPanel.ObtainedCredential credential: credentials)
			setupCredential(entityP, credential);
	}
	
	private void setupCredential(EntityParam entityP, SingleCredentialPanel.ObtainedCredential credential)
	{
		try
		{
			ecredMan.setEntityCredential(entityP, credential.credentialId, credential.secrets);
			if (credential.setAsInvalid)
				ecredMan.setEntityCredentialStatus(entityP, credential.credentialId, 
						LocalCredentialState.outdated);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage(
					"CredentialChangeDialog.credentialUpdateError"), e.getMessage());
		}
	}

}
