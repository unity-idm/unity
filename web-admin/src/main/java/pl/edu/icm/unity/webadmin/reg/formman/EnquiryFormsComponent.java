/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.webui.ActivationListener;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GridContextMenuSupport;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallGrid;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryFormChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormChangedEvent;

/**
 * Responsible for {@link EnquiryForm}s management.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class EnquiryFormsComponent extends VerticalLayout implements ActivationListener
{
	private UnityMessageSource msg;
	private EnquiryManagement enquiriesManagement;
	private EventsBus bus;
	
	private Grid<EnquiryForm> table;
	private com.vaadin.ui.Component main;
	private ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory;
	
	
	@Autowired
	public EnquiryFormsComponent(UnityMessageSource msg, EnquiryManagement enquiryManagement,
			SharedEndpointManagement sharedEndpointMan,
			ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory)
	{
		this.msg = msg;
		this.enquiriesManagement = enquiryManagement;
		this.enquiryFormEditorFactory = enquiryFormEditorFactory;
		this.bus = WebSession.getCurrent().getEventBus();
		
		addStyleName(Styles.visibleScroll.toString());
		setMargin(false);
		setSpacing(false);
		setCaption(msg.getMessage("EnquiryFormsComponent.caption"));
		
		table = new SmallGrid<>();
		table.setSizeFull();
		table.setSelectionMode(SelectionMode.MULTI);
		table.addColumn(EnquiryForm::getName, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationFormsComponent.formsTable"))
			.setId("name");
		table.addComponentColumn(form -> 
			{
				Link link = new Link();
				String linkURL = PublicRegistrationURLSupport.getWellknownEnquiryLink(
						form.getName(), sharedEndpointMan); 
				link.setCaption(linkURL);
				link.setTargetName("_blank");
				link.setResource(new ExternalResource(linkURL));
				return link;
			})
			.setCaption(msg.getMessage("RegistrationFormsComponent.link"))
			.setId("link");
		
		GridContextMenuSupport<EnquiryForm> contextMenu = new GridContextMenuSupport<>(table);
		contextMenu.addActionHandler(getRefreshAction());
		contextMenu.addActionHandler(getAddAction());
		contextMenu.addActionHandler(getEditAction());
		contextMenu.addActionHandler(getCopyAction());
		contextMenu.addActionHandler(getDeleteAction());
		contextMenu.addActionHandler(getResendAction());
		GridSelectionSupport.installClickListener(table);
		table.addItemClickListener(event -> {
			if (event.getMouseEventDetails().isDoubleClick()) 
			{
				EnquiryForm form = event.getItem();
				SingleActionHandler<EnquiryForm> editAction = getEditAction();
				editAction.handle(Sets.newHashSet(form));
			}
		});
		
		
		Toolbar<EnquiryForm> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(contextMenu.getActionHandlers());
		
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setSizeFull();
		
		main = tableWithToolbar;
		refresh();
	}
	
	private void refresh()
	{
		try
		{
			List<EnquiryForm> forms = enquiriesManagement.getEnquires();
			table.setItems(forms);
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
	
	private boolean updateForm(EnquiryForm updatedForm, boolean ignoreRequestsAndInvitations)
	{
		try
		{
			enquiriesManagement.updateEnquiry(updatedForm, ignoreRequestsAndInvitations);
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
	
	private SingleActionHandler<EnquiryForm> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, EnquiryForm.class)
				.withHandler(selection -> refresh())
				.build();
	}
	
	private SingleActionHandler<EnquiryForm> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, EnquiryForm.class)
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
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorInFormEdit"),
					e);
			return;
		}
		EnquiryFormEditDialog dialog = new EnquiryFormEditDialog(msg,
				msg.getMessage("RegistrationFormsComponent.addAction"),
				(eform, foo) -> addForm(eform), editor);
		dialog.show();
	}

	private SingleActionHandler<EnquiryForm> getResendAction()
	{
		return SingleActionHandler.builder(EnquiryForm.class)
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
	
	private SingleActionHandler<EnquiryForm> getCopyAction()
	{
		return SingleActionHandler.builder4Copy(msg, EnquiryForm.class)
				.withHandler(this::showCopyDialog)
				.build();
	}
	
	private SingleActionHandler<EnquiryForm> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, EnquiryForm.class)
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
				caption,(eform, ignoreRequestsAndInvitations) ->  isCopyMode ? addForm(eform)
						: updateForm(eform, ignoreRequestsAndInvitations)
				, editor);
		dialog.show();		
	}
	
	private SingleActionHandler<EnquiryForm> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, EnquiryForm.class)
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
	
	@Override
	public void stateChanged(boolean enabled)
	{
		if (enabled)
			refresh();
	}
}
