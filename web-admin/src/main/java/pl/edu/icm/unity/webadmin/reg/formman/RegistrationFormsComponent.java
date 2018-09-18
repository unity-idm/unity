/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormChangedEvent;

/**
 * Responsible for registration forms management.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class RegistrationFormsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private RegistrationsManagement registrationsManagement;
	private EventsBus bus;
	
	private GenericElementsTable<RegistrationForm> table;
	private com.vaadin.ui.Component main;
	private ObjectFactory<RegistrationFormEditor> editorFactory;
	
	
	@Autowired
	public RegistrationFormsComponent(UnityMessageSource msg, RegistrationsManagement registrationsManagement,
			ObjectFactory<RegistrationFormEditor> editorFactory)
	{
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
		this.editorFactory = editorFactory;
		this.bus = WebSession.getCurrent().getEventBus();
		
		setMargin(false);
		setSpacing(false);
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("RegistrationFormsComponent.caption"));
		
		table = new GenericElementsTable<>(
				msg.getMessage("RegistrationFormsComponent.formsTable"), 
				form -> form.getName());

		table.setSizeFull();
		table.setMultiSelect(true);
		
		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getCopyAction());
		table.addActionHandler(getDeleteAction());
				
		Toolbar<RegistrationForm> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(table.getActionHandlers());
		
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setSizeFull();

		main = tableWithToolbar;
		refresh();
	}
	
	private void refresh()
	{
		try
		{
			List<RegistrationForm> forms = registrationsManagement.getForms();
			table.setInput(forms);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("RegistrationFormsComponent.errorGetForms"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}
	
	private boolean updateForm(RegistrationForm updatedForm, boolean ignoreRequests)
	{
		try
		{
			registrationsManagement.updateForm(updatedForm, ignoreRequests);
			bus.fireEvent(new RegistrationFormChangedEvent(updatedForm));
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorUpdate"), e);
			return false;
		}
	}

	private boolean addForm(RegistrationForm form)
	{
		try
		{
			registrationsManagement.addForm(form);
			bus.fireEvent(new RegistrationFormChangedEvent(form));
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorAdd"), e);
			return false;
		}
	}

	private boolean removeForm(String name, boolean dropRequests)
	{
		try
		{
			registrationsManagement.removeForm(name, dropRequests);
			bus.fireEvent(new RegistrationFormChangedEvent(name));
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorRemove"), e);
			return false;
		}
	}
	
	private SingleActionHandler<RegistrationForm> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, RegistrationForm.class)
				.withHandler(selection -> refresh())
				.build();
	}
	
	private SingleActionHandler<RegistrationForm> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, RegistrationForm.class)
				.withHandler(this::showAddDialog)
				.build();
	}
	
	private void showAddDialog(Set<RegistrationForm> dummy)
	{
		RegistrationFormEditor editor;
		try
		{
			editor = editorFactory.getObject().init(false);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorInFormEdit"), e);
			return;
		}
		RegistrationFormEditDialog dialog = new RegistrationFormEditDialog(msg, 
				msg.getMessage("RegistrationFormsComponent.addAction"), 
				(form, isUpdate) -> addForm(form), editor);
		dialog.show();
	}
	
	private SingleActionHandler<RegistrationForm> getCopyAction()
	{
		return SingleActionHandler.builder4Copy(msg, RegistrationForm.class)
				.withHandler(this::showCopyDialog)
				.build();
	}
	
	private SingleActionHandler<RegistrationForm> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, RegistrationForm.class)
				.withHandler(this::showEditDialog)
				.build();
	}
	
	private void showCopyDialog(Set<RegistrationForm> target)
	{
		showCopyEditDialog(target, true, msg.getMessage("RegistrationFormsComponent.copyAction"));
	}
	
	private void showEditDialog(Set<RegistrationForm> target)
	{
		showCopyEditDialog(target, false, msg.getMessage("RegistrationFormsComponent.editAction"));
	}
	
	private void showCopyEditDialog(Set<RegistrationForm> target, boolean isCopyMode, String caption)
	{
		RegistrationForm targetForm =  target.iterator().next();
		RegistrationForm deepCopy = new RegistrationForm(targetForm.toJson());
		RegistrationFormEditor editor;
		try
		{		
			editor = editorFactory.getObject().init(isCopyMode);
			editor.setForm(deepCopy);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"RegistrationFormsComponent.errorInFormEdit"), e);
			return;
		}
		RegistrationFormEditDialog dialog = new RegistrationFormEditDialog(msg, 
				caption, (form, ignoreRequests) ->
				{
						return isCopyMode ? addForm(form) : updateForm(form, ignoreRequests);
				}, editor);
		dialog.show();	
	}
	
	private SingleActionHandler<RegistrationForm> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, RegistrationForm.class)
				.withHandler(this::handleDelete)
				.build();
	}
	
	private void handleDelete(Set<RegistrationForm> items)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, items);

		new ConfirmWithOptionDialog(msg, msg.getMessage("RegistrationFormsComponent.confirmDelete", 
				confirmText),
				msg.getMessage("RegistrationFormsComponent.dropRequests"),
				new ConfirmWithOptionDialog.Callback()
		{
			@Override
			public void onConfirm(boolean dropRequests)
			{
						for (RegistrationForm item : items)
						{
							removeForm(item.getName(),
									dropRequests);
						}
			}
		}).show();
	}
}
