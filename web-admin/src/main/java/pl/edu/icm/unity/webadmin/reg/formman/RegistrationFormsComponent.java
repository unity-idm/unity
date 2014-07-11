/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.ArrayList;
import java.util.Collection;
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
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webadmin.reg.formman.RegistrationFormEditDialog.Callback;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.registration.RegistrationFormChangedEvent;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
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
	private MessageTemplateManagement msgTempMan;
	private IdentitiesManagement identitiesMan;
	private AttributesManagement attributeMan;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private EventsBus bus;

	
	private GenericElementsTable<RegistrationForm> table;
	private RegistrationFormViewer viewer;
	private com.vaadin.ui.Component main;
	
	
	@Autowired
	public RegistrationFormsComponent(UnityMessageSource msg, RegistrationsManagement registrationsManagement,
			AttributeHandlerRegistry attrHandlersRegistry, GroupsManagement groupsMan, 
			NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan, AuthenticationManagement authenticationMan,
			AttributeHandlerRegistry attrHandlerRegistry)
	{
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
		this.groupsMan = groupsMan;
		this.notificationsMan = notificationsMan;
		this.authenticationMan = authenticationMan;
		this.identitiesMan = identitiesMan;
		this.msgTempMan = msgTempMan;
		this.attributeMan = attributeMan;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.bus = WebSession.getCurrent().getEventBus();
		
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
		table.setMultiSelect(true);
		viewer = new RegistrationFormViewer(msg, attrHandlersRegistry, msgTempMan);
		viewer.setInput(null);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<RegistrationForm> items = getItems(table.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null);
					return;
				}
				RegistrationForm item = items.iterator().next();	
				viewer.setInput(item);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		
		hl.addComponents(tableWithToolbar, viewer);
		hl.setSizeFull();
		hl.setMargin(true);
		hl.setSpacing(true);
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		hl.setExpandRatio(tableWithToolbar, 0.3f);
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
			bus.fireEvent(new RegistrationFormChangedEvent(updatedForm));
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorUpdate"), e);
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
			ErrorPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorAdd"), e);
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
			ErrorPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorRemove"), e);
			return false;
		}
	}
	
	private Collection<RegistrationForm> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<RegistrationForm> items = new ArrayList<RegistrationForm>();
		for (Object o: c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			items.add((RegistrationForm) i.getElement());	
		}	
		return items;
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
						msgTempMan, identitiesMan, attributeMan, authenticationMan,
						attrHandlerRegistry);
			} catch (EngineException e)
			{
				ErrorPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorInFormEdit"), e);
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
			
			GenericItem<?> witem = (GenericItem<?>) target;
			RegistrationForm item = (RegistrationForm) witem.getElement();
			RegistrationFormEditor editor;
			try
			{
				editor = new RegistrationFormEditor(msg, groupsMan, notificationsMan,
						msgTempMan, identitiesMan, attributeMan, authenticationMan,
						attrHandlerRegistry, item);
			} catch (EngineException e)
			{
				ErrorPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorInFormEdit"), e);
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
			setMultiTarget(true);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			final Collection<RegistrationForm> items = getItems(target);
			String confirmText = "";
			for (RegistrationForm item : items)
			{
				confirmText += ", ";
				confirmText += item.getName();
			}
			confirmText = confirmText.substring(2);
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
}
