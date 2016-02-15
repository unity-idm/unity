/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.utils.TimeUtil;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallTable;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.google.common.collect.Lists;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;

/**
 * Table showing invitations, with actions.
 * @author Krzysztof Benedyczak
 */
public class InvitationsTable extends CustomComponent
{
	private UnityMessageSource msg;
	private Table invitationsTable;
	private RegistrationsManagement registrationManagement;
	private NotificationsManagement notificationsManagement;
	private IdentityEditorRegistry identityEditorRegistry;
	private AttributeHandlerRegistry attrHandlersRegistry;
	private AttributesManagement attributesManagement;
	
	public InvitationsTable(UnityMessageSource msg,
			RegistrationsManagement registrationManagement,
			NotificationsManagement notificationsManagement,
			AttributesManagement attributesManagement,
			IdentityEditorRegistry identityEditorRegistry,
			AttributeHandlerRegistry attrHandlersRegistry)
	{
		this.msg = msg;
		this.registrationManagement = registrationManagement;
		this.notificationsManagement = notificationsManagement;
		this.attributesManagement = attributesManagement;
		this.identityEditorRegistry = identityEditorRegistry;
		this.attrHandlersRegistry = attrHandlersRegistry;
		initUI();
	}

	private void initUI()
	{
		invitationsTable = new SmallTable();
		invitationsTable.setNullSelectionAllowed(false);
		invitationsTable.setSizeFull();
		BeanItemContainer<TableInvitationBean> tableContainer = new BeanItemContainer<>(TableInvitationBean.class);
		tableContainer.removeContainerProperty("element");
		invitationsTable.setSelectable(true);
		invitationsTable.setMultiSelect(true);
		invitationsTable.setContainerDataSource(tableContainer);
		invitationsTable.setVisibleColumns(new Object[] {"form", "address", "code", "expiration"});
		invitationsTable.setColumnHeaders(new String[] {
				msg.getMessage("InvitationsTable.form"),
				msg.getMessage("InvitationsTable.contactAddress"),
				msg.getMessage("InvitationsTable.code"),
				msg.getMessage("InvitationsTable.expiration")});
		invitationsTable.setSortContainerPropertyId(invitationsTable.getContainerPropertyIds().iterator().next());
		invitationsTable.setSortAscending(true);

		RefreshActionHandler refreshA = new RefreshActionHandler();
		AddNewActionHandler addA = new AddNewActionHandler();
		SendCodeActionHandler sendA = new SendCodeActionHandler();
		DeleteActionHandler deleteA = new DeleteActionHandler();
		Toolbar toolbar = new Toolbar(invitationsTable, Orientation.HORIZONTAL);
		addAction(refreshA, toolbar);
		addAction(addA, toolbar);
		addAction(sendA, toolbar);
		addAction(deleteA, toolbar);

		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(invitationsTable, toolbar);
		tableWithToolbar.setSizeFull();
		
		setCompositionRoot(tableWithToolbar);
		refresh();
	}

	public void addValueChangeListener(final InvitationSelectionListener listener)
	{
		invitationsTable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				TableInvitationBean selected = getOnlyOneSelected();
				listener.invitationChanged(selected == null ? null : selected.invitation);
			}
		});
	}
	
	private void addAction(SingleActionHandler action, Toolbar toolbar)
	{
		invitationsTable.addActionHandler(action);
		toolbar.addActionHandler(action);
	}
	
	private TableInvitationBean getOnlyOneSelected()
	{
		Collection<?> beans = (Collection<?>) invitationsTable.getValue();
		return beans == null || beans.isEmpty() || beans.size() > 1 ? 
				null : ((TableInvitationBean)beans.iterator().next());
	}
	
	private boolean addInvitation(InvitationParam invitation, boolean send)
	{
		try
		{
			String code = registrationManagement.addInvitation(invitation);
			if (send)
				registrationManagement.sendInvitation(code);
			refresh();
			return true;
		} catch (Exception e)
		{
			String info = msg.getMessage("InvitationsTable.errorAdd");
			NotificationPopup.showError(msg, info, e);
			return false;
		}
	}

	private void removeInvitation(Collection<?> items)
	{
		try
		{
			for (Object item: items)
			{
				TableInvitationBean bean = (TableInvitationBean) item;
				registrationManagement.removeInvitation(bean.getCode());
			}
			refresh();
		} catch (Exception e)
		{
			String info = msg.getMessage("InvitationsTable.errorDelete");
			NotificationPopup.showError(msg, info, e);
		}
	}

	private void sendInvitation(Collection<?> items)
	{
		try
		{
			for (Object item: items)
			{
				TableInvitationBean bean = (TableInvitationBean) item;
				registrationManagement.sendInvitation(bean.getCode());
			}
			refresh();
		} catch (Exception e)
		{
			String info = msg.getMessage("InvitationsTable.errorSend");
			NotificationPopup.showError(msg, info, e);
		}
	}
	
	private Collection<RegistrationForm> getForms() throws EngineException
	{
		return registrationManagement.getForms();
	}
	
	private Collection<String> getChannels() throws EngineException
	{
		return notificationsManagement.getNotificationChannels().keySet();
	}
	
	private void refresh()
	{
		try
		{
			TableInvitationBean selected = getOnlyOneSelected();
			List<InvitationWithCode> invitations = registrationManagement.getInvitations();
			invitationsTable.removeAllItems();
			for (InvitationWithCode invitation: invitations)
			{
				TableInvitationBean item = new TableInvitationBean(invitation);
				invitationsTable.addItem(item);
				if (selected != null && selected.getCode().equals(invitation.getRegistrationCode()))
					invitationsTable.setValue(Lists.newArrayList(item));
			}
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("InvitationsTable.errorGetInvitations"), e);
			setCompositionRoot(error);
		}
	}

	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("InvitationsTable.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}

	private class AddNewActionHandler extends SingleActionHandler
	{
		public AddNewActionHandler()
		{
			super(msg.getMessage("InvitationsTable.addInvitationAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			
			InvitationEditor editor;
			try
			{
				editor = new InvitationEditor(msg, identityEditorRegistry, attrHandlersRegistry, 
						getForms(), getChannels(), attributesManagement.getAttributeTypesAsMap());
			} catch (WrongArgumentException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("InvitationsTable.noValidForms"), 
						msg.getMessage("InvitationsTable.noValidFormsDesc"));
				return;
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("InvitationsTable.errorGetData"), e);
				return;
			}
			InvitationEditDialog dialog = new InvitationEditDialog(msg, 
					msg.getMessage("InvitationsTable.addInvitationAction"), editor, 
					(invitation, sendInvitation) -> addInvitation(invitation, sendInvitation));
			dialog.show();
		}
	}

	
	private class SendCodeActionHandler extends SingleActionHandler
	{
		public SendCodeActionHandler()
		{
			super(msg.getMessage("InvitationsTable.sendCodeAction"), Images.messageSend.getResource());
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			final Collection<?> items = (Collection<?>) invitationsTable.getValue();
			sendInvitation(items);
		}
	}
	
	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("InvitationsTable.deleteAction"), Images.delete.getResource());
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			final Collection<?> items = (Collection<?>) invitationsTable.getValue();
			new ConfirmDialog(msg, msg.getMessage(
					"InvitationsTable.confirmDelete", items.size()),
					new ConfirmDialog.Callback()
					{
						@Override
						public void onConfirm()
						{
							removeInvitation(items);
						}
					}).show();
		}
	}
	
	public static class TableInvitationBean
	{
		private InvitationWithCode invitation;
		
		public TableInvitationBean(InvitationWithCode invitation)
		{
			this.invitation = invitation;
		}

		public String getForm()
		{
			return invitation.getFormId();
		}
		
		public String getCode()
		{
			return invitation.getRegistrationCode();
		}
		
		public Label getExpiration()
		{
			Label ret = new Label(TimeUtil.formatMediumInstant(invitation.getExpiration()));
			if (Instant.now().isAfter(invitation.getExpiration()))
				ret.addStyleName(Styles.error.toString());
			return ret;
		}
		
		public String getAddress()
		{
			return invitation.getContactAddress() == null ? "-" : invitation.getContactAddress();
		}
	}
	
	public interface InvitationSelectionListener
	{
		void invitationChanged(InvitationWithCode invitation);
	}

}
