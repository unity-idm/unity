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
import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.webadmin.reg.formman.EnquiryFormEditDialog.Callback;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentFactory;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.registration.EnquiryFormChangedEvent;
import pl.edu.icm.unity.webui.registration.RegistrationFormChangedEvent;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Responsible for {@link EnquiryForm}s management.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EnquiryFormsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private EnquiryManagement enquiriesManagement;
	private GroupsManagement groupsMan;
	private NotificationsManagement notificationsMan;
	private AuthenticationManagement authenticationMan;
	private MessageTemplateManagement msgTempMan;
	private IdentitiesManagement identitiesMan;
	private AttributesManagement attributeMan;
	private EventsBus bus;
	private ActionParameterComponentFactory actionComponentFactory;
	
	private GenericElementsTable<EnquiryForm> table;
	private EnquiryFormViewer viewer;
	private com.vaadin.ui.Component main;
	private RegistrationActionsRegistry actionsRegistry;
	
	
	@Autowired
	public EnquiryFormsComponent(UnityMessageSource msg, EnquiryManagement enquiryManagement,
			AttributeHandlerRegistry attrHandlersRegistry, GroupsManagement groupsMan, 
			NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan, AuthenticationManagement authenticationMan,
			SharedEndpointManagement sharedEndpointMan,
			RegistrationActionsRegistry actionsRegistry,
			ActionParameterComponentFactory actionComponentFactory)
	{
		this.msg = msg;
		this.enquiriesManagement = enquiryManagement;
		this.groupsMan = groupsMan;
		this.notificationsMan = notificationsMan;
		this.authenticationMan = authenticationMan;
		this.identitiesMan = identitiesMan;
		this.msgTempMan = msgTempMan;
		this.attributeMan = attributeMan;
		this.actionsRegistry = actionsRegistry;
		this.actionComponentFactory = actionComponentFactory;
		this.bus = WebSession.getCurrent().getEventBus();
		
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("EnquiryFormsComponent.caption"));
		table = new GenericElementsTable<EnquiryForm>(msg.getMessage("RegistrationFormsComponent.formsTable"), 
				new GenericElementsTable.NameProvider<EnquiryForm>()
				{
					@Override
					public Label toRepresentation(EnquiryForm element)
					{
						return new Label(element.getName());
					}
				});
		table.setSizeFull();
		table.setMultiSelect(true);
		viewer = new EnquiryFormViewer(msg, actionsRegistry, msgTempMan);
		viewer.setInput(null);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<EnquiryForm> items = getItems(table.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null);
					return;
				}
				EnquiryForm item = items.iterator().next();	
				viewer.setInput(item);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new CopyActionHandler());
		table.addActionHandler(new DeleteActionHandler());
				
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setSizeFull();
		
		CompositeSplitPanel hl = new CompositeSplitPanel(false, true, tableWithToolbar, viewer, 25);

		main = hl;
		refresh();
	}
	
	private void refresh()
	{
		try
		{
			List<EnquiryForm> forms = enquiriesManagement.getEnquires();
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
	
	private boolean updateForm(EnquiryForm updatedForm, boolean ignoreRequests)
	{
		try
		{
			enquiriesManagement.updateEnquiry(updatedForm, ignoreRequests);
			bus.fireEvent(new EnquiryFormChangedEvent(updatedForm));
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorUpdate"), e);
			return false;
		}
	}

	private boolean addForm(EnquiryForm form)
	{
		try
		{
			enquiriesManagement.addEnquiry(form);
			bus.fireEvent(new EnquiryFormChangedEvent(form));
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
			enquiriesManagement.removeEnquiry(name, dropRequests);
			bus.fireEvent(new RegistrationFormChangedEvent(name));
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorRemove"), e);
			return false;
		}
	}
	
	private Collection<EnquiryForm> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<EnquiryForm> items = new ArrayList<>();
		for (Object o: c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			items.add((EnquiryForm) i.getElement());	
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
			EnquiryFormEditor editor;
			try
			{
				editor = new EnquiryFormEditor(msg, groupsMan, notificationsMan,
						msgTempMan, identitiesMan, attributeMan, authenticationMan,
						actionsRegistry, actionComponentFactory.getComponentProvider());
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorInFormEdit"), e);
				return;
			}
			EnquiryFormEditDialog dialog = new EnquiryFormEditDialog(msg, 
					msg.getMessage("RegistrationFormsComponent.addAction"), new Callback()
					{
						@Override
						public boolean newForm(EnquiryForm form, boolean foo)
						{
							return addForm(form);
						}
					}, editor);
			dialog.show();
		}
	}

	private class EditActionHandler extends CopyEditBaseActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.editAction"), 
					Images.edit.getResource(), false);
		}
	}
	
	private class CopyActionHandler extends CopyEditBaseActionHandler
	{
		public CopyActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.copyAction"), 
					Images.copy.getResource(), true);
		}
	}

	
	private abstract class CopyEditBaseActionHandler extends SingleActionHandler
	{
		private boolean copyMode;
		private String caption;

		public CopyEditBaseActionHandler(String caption, Resource icon, boolean copyMode)
		{
			super(caption, icon);
			this.caption = caption;
			this.copyMode = copyMode;
		}

		@Override
		protected void handleAction(Object sender, final Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<EnquiryForm> item = (GenericItem<EnquiryForm>) target;
			EnquiryForm form =  item.getElement();
			EnquiryFormEditor editor;
			try
			{		
				editor = new EnquiryFormEditor(msg, groupsMan, notificationsMan,
						msgTempMan, identitiesMan, attributeMan, authenticationMan,
						actionsRegistry, actionComponentFactory.getComponentProvider(), 
						copyMode);
				editor.setForm(form);
			} catch (Exception e)
			{
				NotificationPopup.showError(msg, msg.getMessage(
						"RegistrationFormsComponent.errorInFormEdit"), e);
				return;
			}
			EnquiryFormEditDialog dialog = new EnquiryFormEditDialog(msg, 
					caption, new Callback()
					{
						@Override
						public boolean newForm(EnquiryForm form, boolean ignoreRequests)
						{
							return copyMode ? addForm(form) :
								updateForm(form, ignoreRequests);
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
			final Collection<EnquiryForm> items = getItems(target);
			String confirmText = MessageUtils.createConfirmFromNames(msg, items);

			new ConfirmWithOptionDialog(msg, msg.getMessage("RegistrationFormsComponent.confirmDelete", 
					confirmText),
					msg.getMessage("RegistrationFormsComponent.dropRequests"),
					new ConfirmWithOptionDialog.Callback()
			{
				@Override
				public void onConfirm(boolean dropRequests)
				{
							for (EnquiryForm item : items)
							{
								removeForm(item.getName(),
										dropRequests);
							}
				}
			}).show();
		}
	}
}
