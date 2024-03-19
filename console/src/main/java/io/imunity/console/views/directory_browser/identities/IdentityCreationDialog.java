/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorContext;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

class IdentityCreationDialog extends DialogWithActionFooter
{
	private long entityId;
	protected MessageSource msg;
	protected NotificationPresenter notificationPresenter;
	protected EntityManagement identitiesMan;
	protected IdentityEditorRegistry identityEditorReg;
	protected Consumer<Identity> callback;

	protected ComboBox<String> identityTypeSelector;
	protected IdentityEditor identityEditor;
	private FormLayout idLayout;


	IdentityCreationDialog(MessageSource msg, long entityId, EntityManagement identitiesMan,
			IdentityEditorRegistry identityEditorReg, Consumer<Identity> callback,
			NotificationPresenter notificationPresenter)
	{
		this(msg.getMessage("IdentityCreation.caption"), msg, identitiesMan, identityEditorReg, callback,
				notificationPresenter);
		this.entityId = entityId;
	}

	IdentityCreationDialog(String caption, MessageSource msg, EntityManagement identitiesMan,
			IdentityEditorRegistry identityEditorReg, Consumer<Identity> callback,
			NotificationPresenter notificationPresenter)
	{
		super(msg::getMessage);
		this.msg = msg;
		this.identityEditorReg = identityEditorReg;
		this.identitiesMan = identitiesMan;
		this.callback = callback;
		this.notificationPresenter = notificationPresenter;
		setHeaderTitle(caption);
		setActionButton(msg.getMessage("ok"), this::onConfirm);
		setWidth("35em");
		setHeight("26em");
		add(getContents());
	}

	@Component
	static class IdentityCreationDialogHandler
	{
		private final MessageSource msg;
		private final EntityManagement identitiesMan;
		private final IdentityEditorRegistry identityEditorReg;
		private final NotificationPresenter notificationPresenter;

		IdentityCreationDialogHandler(MessageSource msg, EntityManagement identitiesMan,
				IdentityEditorRegistry identityEditorReg, NotificationPresenter notificationPresenter)
		{
			this.msg = msg;
			this.identitiesMan = identitiesMan;
			this.identityEditorReg = identityEditorReg;
			this.notificationPresenter = notificationPresenter;
		}

		SingleActionHandler<IdentityEntry> getAction(Consumer<Identity> callback)
		{
			return SingleActionHandler.builder(IdentityEntry.class)
					.withCaption(msg.getMessage("Identities.addIdentityAction"))
					.withIcon(VaadinIcon.USER_CARD)
					.withHandler(selection -> showAddIdentityDialog(selection, callback))
					.build();
		}

		private void showAddIdentityDialog(Collection<IdentityEntry> selection, Consumer<Identity> callback)
		{
			IdentityEntry selected = selection.iterator().next();
			long entityId = selected.getSourceEntity().getEntity().getId();
			new IdentityCreationDialog(msg, entityId, identitiesMan,
					identityEditorReg, callback, notificationPresenter).open();
		}
	}


	private VerticalLayout getContents()
	{
		FormLayout typeSelectionLayout = new FormLayout();
		identityTypeSelector = new ComboBox<>();
		List<String> supportedTypes = new ArrayList<>(identityEditorReg.getSupportedTypes());
		supportedTypes.sort(String::compareTo);
		identityTypeSelector.setItems(supportedTypes);

		typeSelectionLayout.addFormItem(identityTypeSelector, msg.getMessage("IdentityCreation.idType"));

		idLayout  = new FormLayout();
		idLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		identityTypeSelector.addValueChangeListener(event ->
		{
			String type = identityTypeSelector.getValue();
			IdentityEditor editor = identityEditorReg.getEditor(type);
			idLayout.removeAll();
			ComponentsContainer container = editor.getEditor(IdentityEditorContext.builder()
					.withRequired(true)
					.withAdminMode(true).build());
			Arrays.stream(container.getComponents()).forEach(item ->
			{
				if (item instanceof CustomField<?> field)
				{
					idLayout.addFormItem(field, field.getLabel());
					field.getElement().setAttribute("required", true);
					field.setLabel(null);
				} else
				{
					idLayout.addFormItem(item, item.getElement().getProperty("label"));
					item.getElement().setProperty("label", null);
				}
			});
			identityEditor = editor;
		});
		identityTypeSelector.setValue(supportedTypes.iterator().next());

		VerticalLayout main = new VerticalLayout();
		main.add(typeSelectionLayout, idLayout);
		main.setSizeFull();
		return main;
	}

	protected void onConfirm()
	{
		IdentityParam toAdd;
		try
		{
			toAdd = identityEditor.getValue();
		} catch (IllegalIdentityValueException e)
		{
			open();
			idLayout.getChildren().forEach(child -> child.getElement().setAttribute("invalid", true));
			return;
		}
		Identity added;
		try
		{
			added = identitiesMan.addIdentity(toAdd, new EntityParam(entityId));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("IdentityCreation.entityCreateError"), e.getMessage());
			return;
		}
		callback.accept(added);
	}
}
