/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webadmin.reg.formman.RegistrationFormEditDialog.Callback;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Responsible for registration forms management.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RegistrationFormsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private RegistrationsManagement registrationsManagement;
	private GroupsManagement groupsMan;
	private NotificationsManagement notificationsMan;
	private AuthenticationManagement authenticationMan;
	private IdentitiesManagement identitiesMan;
	private UnityServerConfiguration serverCfg;
	private AttributesManagement attributeMan;
	private AttributeHandlerRegistry attrHandlerRegistry;

	
	private GenericElementsTable<RegistrationForm> table;
	private RegistrationFormViewer viewer;
	private com.vaadin.ui.Component main;
	
	
	@Autowired
	public RegistrationFormsComponent(UnityMessageSource msg, RegistrationsManagement registrationsManagement,
			AttributeHandlerRegistry attrHandlersRegistry, GroupsManagement groupsMan, 
			NotificationsManagement notificationsMan,
			UnityServerConfiguration cfg, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan, AuthenticationManagement authenticationMan,
			AttributeHandlerRegistry attrHandlerRegistry)
	{
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
		this.groupsMan = groupsMan;
		this.notificationsMan = notificationsMan;
		this.authenticationMan = authenticationMan;
		this.identitiesMan = identitiesMan;
		this.serverCfg = cfg;
		this.attributeMan = attributeMan;
		this.attrHandlerRegistry = attrHandlerRegistry;
		
		HorizontalLayout hl = new HorizontalLayout();
		setCaption(msg.getMessage("RegistrationFormsComponent.caption"));
		table = new GenericElementsTable<RegistrationForm>(msg.getMessage("RegistrationFormsComponent.formsTable"), 
				RegistrationForm.class, new GenericElementsTable.NameProvider<RegistrationForm>()
				{
					@Override
					public Label toRepresentation(RegistrationForm element)
					{
						return new Label(element.getName());
					}
				});
		table.setWidth(90, Unit.PERCENTAGE);
		hl.addComponent(table);
		viewer = new RegistrationFormViewer(msg, attrHandlersRegistry, serverCfg.getTemplatesStore());
		viewer.setInput(null);
		hl.addComponent(viewer);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				@SuppressWarnings("unchecked")
				GenericItem<RegistrationForm> item = (GenericItem<RegistrationForm>)table.getValue();
				if (item != null)
				{
					RegistrationForm form = item.getElement();
					viewer.setInput(form);
				} else
					viewer.setInput(null);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		hl.setSizeFull();
		hl.setMargin(true);
		hl.setSpacing(true);
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		hl.setExpandRatio(table, 0.3f);
		hl.setExpandRatio(viewer, 0.7f);
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
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("RegistrationFormsComponent.errorUpdate"), e);
			return false;
		}
	}

	private boolean addForm(RegistrationForm form)
	{
		try
		{
			registrationsManagement.addForm(form);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("RegistrationFormsComponent.errorAdd"), e);
			return false;
		}
	}

	private boolean removeForm(String name, boolean dropRequests)
	{
		try
		{
			registrationsManagement.removeForm(name, dropRequests);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("RegistrationFormsComponent.errorRemove"), e);
			return false;
		}
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}


	private class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			RegistrationFormEditor editor;
			try
			{
				editor = new RegistrationFormEditor(msg, groupsMan, notificationsMan,
						serverCfg, identitiesMan, attributeMan, authenticationMan,
						attrHandlerRegistry);
			} catch (EngineException e)
			{
				ErrorPopup.showError(msg.getMessage("RegistrationFormsComponent.errorInFormEdit"), e);
				return;
			}
			RegistrationFormEditDialog dialog = new RegistrationFormEditDialog(msg, 
					msg.getMessage("RegistrationFormsComponent.addAction"), new Callback()
					{
						@Override
						public boolean newForm(RegistrationForm form, boolean foo)
						{
							return addForm(form);
						}
					}, editor);
			dialog.show();
		}
	}


	private class EditActionHandler extends SingleActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.editAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<RegistrationForm> item = (GenericItem<RegistrationForm>) target;
			RegistrationFormEditor editor;
			try
			{
				editor = new RegistrationFormEditor(msg, groupsMan, notificationsMan,
						serverCfg, identitiesMan, attributeMan, authenticationMan,
						attrHandlerRegistry, item.getElement());
			} catch (EngineException e)
			{
				ErrorPopup.showError(msg.getMessage("RegistrationFormsComponent.errorInFormEdit"), e);
				return;
			}
			RegistrationFormEditDialog dialog = new RegistrationFormEditDialog(msg, 
					msg.getMessage("RegistrationFormsComponent.editAction"), new Callback()
					{
						@Override
						public boolean newForm(RegistrationForm form, boolean ignoreRequests)
						{
							return updateForm(form, ignoreRequests);
						}
					}, editor);
			dialog.show();
		}
	}
	
	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.deleteAction"), 
					Images.delete.getResource());
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<RegistrationForm> item = (GenericItem<RegistrationForm>)target;
			final RegistrationForm form = item.getElement();
			new ConfirmWithOptionDialog(msg, msg.getMessage("RegistrationFormsComponent.confirmDelete", 
					form.getName()),
					msg.getMessage("RegistrationFormsComponent.dropRequests"),
					new ConfirmWithOptionDialog.Callback()
			{
				@Override
				public void onConfirm(boolean dropRequests)
				{
					removeForm(form.getName(), dropRequests);
				}
			}).show();
		}
	}
}
