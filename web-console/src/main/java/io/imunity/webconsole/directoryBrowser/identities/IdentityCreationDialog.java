/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorContext;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistryV8;

/**
 * Identity creation dialog. Adds the identity to an existing entity.
 * @author K. Benedyczak
 */
class IdentityCreationDialog extends AbstractDialog
{
	private long entityId;
	protected EntityManagement identitiesMan;
	protected IdentityEditorRegistryV8 identityEditorReg;
	protected Consumer<Identity> callback;
	
	protected ComboBox<String> identityTypeSelector;
	protected IdentityEditor identityEditor;
	
	IdentityCreationDialog(MessageSource msg, long entityId, EntityManagement identitiesMan,
	                       IdentityEditorRegistryV8 identityEditorReg, Consumer<Identity> callback)
	{
		this(msg.getMessage("IdentityCreation.caption"), msg, identitiesMan, identityEditorReg, callback);
		this.entityId = entityId;
	}

	protected IdentityCreationDialog(String caption, MessageSource msg, EntityManagement identitiesMan,
	                                 IdentityEditorRegistryV8 identityEditorReg, Consumer<Identity> callback)
	{
		super(msg, caption);
		this.identityEditorReg = identityEditorReg;
		this.identitiesMan = identitiesMan;
		this.callback = callback;
	}

	@Component
	static class IdentityCreationDialogHandler
	{
		@Autowired
		private MessageSource msg;
		@Autowired
		private EntityManagement identitiesMan;
		@Autowired
		private IdentityEditorRegistryV8 identityEditorReg;
		
		SingleActionHandler<IdentityEntry> getAction(Consumer<Identity> callback)
		{
			return SingleActionHandler.builder(IdentityEntry.class)
					.withCaption(msg.getMessage("Identities.addIdentityAction"))
					.withIcon(Images.addIdentity.getResource())
					.withHandler(selection -> showAddIdentityDialog(selection, callback))
					.build();
		}
		
		private void showAddIdentityDialog(Collection<IdentityEntry> selection, Consumer<Identity> callback)
		{
			IdentityEntry selected = selection.iterator().next();
			long entityId = selected.getSourceEntity().getEntity().getId();
			new IdentityCreationDialog(msg, entityId, identitiesMan, 
					identityEditorReg, callback).show();
		}
	}

	
	@Override
	protected AbstractOrderedLayout getContents() throws EngineException
	{
		setSizeEm(50, 30);
		
		FormLayout typeSelectionLayout = FormLayoutWithFixedCaptionWidth.withShortCaptions();
		typeSelectionLayout.setMargin(new MarginInfo(false));
		identityTypeSelector = new ComboBox<>(msg.getMessage("IdentityCreation.idType"));
		List<String> supportedTypes = new ArrayList<>(identityEditorReg.getSupportedTypes());
		supportedTypes.sort(String::compareTo);
		identityTypeSelector.setItems(supportedTypes);
		identityTypeSelector.setEmptySelectionAllowed(false);
		
		typeSelectionLayout.addComponent(identityTypeSelector);

		FormLayout idLayout = FormLayoutWithFixedCaptionWidth.withShortCaptions();
		idLayout.setMargin(new MarginInfo(false));
		
		identityTypeSelector.addValueChangeListener(event -> 
		{
			String type = identityTypeSelector.getValue();
			IdentityEditor editor = identityEditorReg.getEditor(type);
			idLayout.removeAllComponents();
			ComponentsContainer container = editor.getEditor(IdentityEditorContext.builder()
					.withRequired(true)
					.withAdminMode(true).build());
			idLayout.addComponents(container.getComponents());
			identityEditor = editor;
		});
		identityTypeSelector.setSelectedItem(supportedTypes.iterator().next());

		VerticalLayout main = new VerticalLayout();
		main.addComponents(typeSelectionLayout, idLayout);
		main.setSizeFull();
		return main;
	}

	@Override
	protected void onConfirm()
	{
		IdentityParam toAdd;
		try
		{
			toAdd = identityEditor.getValue();
		} catch (IllegalIdentityValueException e)
		{
			return;
		}
		Identity added = null;
		try
		{
			added = identitiesMan.addIdentity(toAdd, new EntityParam(entityId));
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("IdentityCreation.entityCreateError"), e);
			return;
		}
		
		callback.accept(added);
		close();
	}
}
