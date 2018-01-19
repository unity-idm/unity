/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.webadmin.reg.formman.EnquiryFormEditDialog.Callback;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler2;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryFormChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormChangedEvent;

/**
 * Responsible for {@link EnquiryForm}s management.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class EnquiryFormsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private EnquiryManagement enquiriesManagement;
	private EventsBus bus;
	
	private GenericElementsTable<EnquiryForm> table;
	private com.vaadin.ui.Component main;
	private ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory;
	
	
	@Autowired
	public EnquiryFormsComponent(UnityMessageSource msg, EnquiryManagement enquiryManagement,
			SharedEndpointManagement sharedEndpointMan,
			ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory,
			EnquiryFormViewer viewer)
	{
		this.msg = msg;
		this.enquiriesManagement = enquiryManagement;
		this.enquiryFormEditorFactory = enquiryFormEditorFactory;
		this.bus = WebSession.getCurrent().getEventBus();
		
		addStyleName(Styles.visibleScroll.toString());
		setMargin(false);
		setSpacing(false);
		setCaption(msg.getMessage("EnquiryFormsComponent.caption"));
		
		
		table = new GenericElementsTable<>(msg.getMessage("RegistrationFormsComponent.formsTable"), 
				form -> form.getName());
		table.setSizeFull();
		table.setMultiSelect(true);
		viewer.setInput(null);
		table.addSelectionListener(event -> 
		{
			Collection<EnquiryForm> items = event.getAllSelectedItems();
			if (items.size() > 1 || items.isEmpty())
			{
				viewer.setInput(null);
				return;
			}
			EnquiryForm item = items.iterator().next();	
			viewer.setInput(item);
		});
		
		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getCopyAction());
		table.addActionHandler(getDeleteAction());
		table.addActionHandler(getResendAction());
				
		Toolbar<EnquiryForm> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
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

	private void resend(String name)
	{
		try
		{
			enquiriesManagement.sendEnquiry(name);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorSend"), e);
		}
	}
	
	private SingleActionHandler2<EnquiryForm> getRefreshAction()
	{
		return SingleActionHandler2.builder4Refresh(msg, EnquiryForm.class)
				.withHandler(selection -> refresh())
				.build();
	}
	
	private SingleActionHandler2<EnquiryForm> getAddAction()
	{
		return SingleActionHandler2.builder4Add(msg, EnquiryForm.class)
				.withHandler(this::showAddDialog)
				.build();
	}
	
	private void showAddDialog(Set<EnquiryForm> form)
	{
		EnquiryFormEditor editor;
		try
		{
			editor = enquiryFormEditorFactory.getObject().init(false);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, 
					msg.getMessage("RegistrationFormsComponent.errorInFormEdit"), e);
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
	
	private SingleActionHandler2<EnquiryForm> getResendAction()
	{
		return SingleActionHandler2.builder(EnquiryForm.class)
				.withCaption(msg.getMessage("RegistrationFormsComponent.resendAction"))
				.withIcon(Images.messageSend.getResource())
				.withHandler(this::showResendDialog)
				.build();
	}
	
	public void showResendDialog(Set<EnquiryForm> forms)
	{
		EnquiryForm form = forms.iterator().next();
		ConfirmDialog dialog = new ConfirmDialog(msg, 
				msg.getMessage("RegistrationFormsComponent.resendConfirmation"), 
				() -> resend(form.getName()));
		dialog.show();
	}
	
	private SingleActionHandler2<EnquiryForm> getCopyAction()
	{
		return SingleActionHandler2.builder4Copy(msg, EnquiryForm.class)
				.withHandler(this::showCopyDialog)
				.build();
	}
	
	private SingleActionHandler2<EnquiryForm> getEditAction()
	{
		return SingleActionHandler2.builder4Edit(msg, EnquiryForm.class)
				.withHandler(this::showEditDialog)
				.build();
	}
	
	private void showCopyDialog(Set<EnquiryForm> target)
	{
		showCopyEditDialog(target, true, msg.getMessage("RegistrationFormsComponent.copyAction"));
	}
	
	private void showEditDialog(Set<EnquiryForm> target)
	{
		showCopyEditDialog(target, false, msg.getMessage("RegistrationFormsComponent.editAction"));
	}
	
	private void showCopyEditDialog(Set<EnquiryForm> target, boolean isCopyMode, String caption)
	{
		EnquiryForm form =  target.iterator().next();
		EnquiryFormEditor editor;
		try
		{		
			editor = enquiryFormEditorFactory.getObject().init(isCopyMode);
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
						return isCopyMode ? addForm(form) :
							updateForm(form, ignoreRequests);
					}
				}, editor);
		dialog.show();		
	}
	
	private SingleActionHandler2<EnquiryForm> getDeleteAction()
	{
		return SingleActionHandler2.builder4Delete(msg, EnquiryForm.class)
				.withHandler(this::handleDelete)
				.build();
	}
	
	private void handleDelete(Set<EnquiryForm> items)
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
						for (EnquiryForm item : items)
						{
							removeForm(item.getName(),
									dropRequests);
						}
			}
		}).show();
	}
}
