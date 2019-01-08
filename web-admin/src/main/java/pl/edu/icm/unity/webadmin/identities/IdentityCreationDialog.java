/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorContext;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Identity creation dialog. Adds the identity to an existing entity.
 * @author K. Benedyczak
 */
public class IdentityCreationDialog extends AbstractDialog
{
	private long entityId;
	protected EntityManagement identitiesMan;
	protected IdentityEditorRegistry identityEditorReg;
	protected Consumer<Identity> callback;
	
	protected ComboBox<String> identityType;
	protected IdentityEditor identityEditor;
	protected CheckBox extractAttributes;
	
	public IdentityCreationDialog(UnityMessageSource msg, long entityId, EntityManagement identitiesMan,
			IdentityEditorRegistry identityEditorReg, Consumer<Identity> callback)
	{
		this(msg.getMessage("IdentityCreation.caption"), msg, identitiesMan, identityEditorReg, callback);
		this.entityId = entityId;
	}

	protected IdentityCreationDialog(String caption, UnityMessageSource msg, EntityManagement identitiesMan,
			IdentityEditorRegistry identityEditorReg, Consumer<Identity> callback)
	{
		super(msg, caption);
		this.identityEditorReg = identityEditorReg;
		this.identitiesMan = identitiesMan;
		this.callback = callback;
	}

	@Component
	public static class IdentityCreationDialogHandler
	{
		@Autowired
		private UnityMessageSource msg;
		@Autowired
		private EntityManagement identitiesMan;
		@Autowired
		private IdentityEditorRegistry identityEditorReg;
		
		public SingleActionHandler<IdentityEntry> getAction(Consumer<Identity> callback)
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
	protected FormLayout getContents() throws EngineException
	{
		setSize(50, 50);
		identityType = new ComboBox<>(msg.getMessage("IdentityCreation.idType"));
		Set<String> supportedTypes = identityEditorReg.getSupportedTypes();
		identityType.setItems(supportedTypes);
		identityType.setEmptySelectionAllowed(false);

		Panel identityPanel = new SafePanel(msg.getMessage("IdentityCreation.idValue"));
		final FormLayout idLayout = new CompactFormLayout();
		idLayout.setMargin(true);
		identityPanel.setContent(idLayout);
		
		identityType.addValueChangeListener(event -> 
		{
			String type = identityType.getValue();
			IdentityEditor editor = identityEditorReg.getEditor(type);
			idLayout.removeAllComponents();
			ComponentsContainer container = editor.getEditor(IdentityEditorContext.builder()
					.withRequired(true)
					.withAdminMode(true).build());
			idLayout.addComponents(container.getComponents());
			identityEditor = editor;
		});
		identityType.setSelectedItem(supportedTypes.iterator().next());

		extractAttributes = new CheckBox(msg.getMessage("IdentityCreation.extractAttrs"), true);

		FormLayout main = new CompactFormLayout();
		main.addComponents(identityType, identityPanel, extractAttributes);
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
			added = identitiesMan.addIdentity(toAdd, new EntityParam(entityId), 
					extractAttributes.getValue());
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("IdentityCreation.entityCreateError"), e);
			return;
		}
		
		callback.accept(added);
		close();
	}
}
