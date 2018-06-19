/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.data.ValueProvider;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GridContextMenuSupport;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallGrid;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Table showing invitations, with actions.
 * @author Krzysztof Benedyczak
 */
public class InvitationsTable extends CustomComponent
{
	private UnityMessageSource msg;
	private Grid<TableInvitationBean> invitationsTable;
	private RegistrationsManagement registrationManagement;
	private InvitationManagement invitationManagement;
	private IdentityEditorRegistry identityEditorRegistry;
	private AttributeHandlerRegistry attrHandlersRegistry;
	private AttributeTypeManagement attributesManagement;
	private MessageTemplateManagement msgTemplateManagement;
	
	public InvitationsTable(UnityMessageSource msg,
			RegistrationsManagement registrationManagement,
			InvitationManagement invitationManagement,
			AttributeTypeManagement attributesManagement,
			IdentityEditorRegistry identityEditorRegistry,
			AttributeHandlerRegistry attrHandlersRegistry,
			MessageTemplateManagement msgTemplateManagement)
	{
		this.msg = msg;
		this.registrationManagement = registrationManagement;
		this.invitationManagement = invitationManagement;
		this.attributesManagement = attributesManagement;
		this.identityEditorRegistry = identityEditorRegistry;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.msgTemplateManagement = msgTemplateManagement;
		initUI();
	}

	private void initUI()
	{
		invitationsTable = new SmallGrid<>();
		invitationsTable.setSizeFull();
		invitationsTable.setSelectionMode(SelectionMode.MULTI);
		
		invitationsTable.addColumn(TableInvitationBean::getForm, ValueProvider.identity())
			.setCaption(msg.getMessage("InvitationsTable.form"))
			.setId("form");
		invitationsTable.addColumn(TableInvitationBean::getAddress, ValueProvider.identity())
			.setCaption(msg.getMessage("InvitationsTable.contactAddress"))
			.setId("contactAddress");
		invitationsTable.addColumn(TableInvitationBean::getCode, ValueProvider.identity())
			.setCaption(msg.getMessage("InvitationsTable.code"))
			.setId("code");
		invitationsTable.addColumn(TableInvitationBean::getExpiration, ValueProvider.identity())
			.setCaption(msg.getMessage("InvitationsTable.expiration"))
			.setStyleGenerator(invitation -> invitation.isExpired() ? Styles.error.toString() : null)
			.setId("expiration");
		
		invitationsTable.sort("contactAddress", SortDirection.ASCENDING);
		
		GridContextMenuSupport<TableInvitationBean> contextMenu = new GridContextMenuSupport<>(invitationsTable);
		contextMenu.addActionHandler(getRefreshAction());
		contextMenu.addActionHandler(getAddAction());
		contextMenu.addActionHandler(getSendAction());
		contextMenu.addActionHandler(getDeleteAction());
		GridSelectionSupport.installClickListener(invitationsTable);
		
		Toolbar<TableInvitationBean> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		invitationsTable.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(contextMenu.getActionHandlers());
		
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(invitationsTable, toolbar);
		tableWithToolbar.setSizeFull();
		
		setCompositionRoot(tableWithToolbar);
		refresh();
	}
	
	public void addValueChangeListener(final InvitationSelectionListener listener)
	{
		invitationsTable.addSelectionListener(event ->
		{
			TableInvitationBean selected = getOnlyOneSelected();
			listener.invitationChanged(selected == null ? null : selected.invitation);
		});
	}
	
	private TableInvitationBean getOnlyOneSelected()
	{
		Collection<TableInvitationBean> beans = invitationsTable.getSelectedItems();
		return beans == null || beans.isEmpty() || beans.size() > 1 ? 
				null : ((TableInvitationBean)beans.iterator().next());
	}
	
	private boolean addInvitation(InvitationParam invitation, boolean send)
	{
		String code;
		try
		{
			code = invitationManagement.addInvitation(invitation);
			refresh();
		} catch (Exception e)
		{
			String info = msg.getMessage("InvitationsTable.errorAdd");
			NotificationPopup.showError(msg, info, e);
			return false;
		}
		if (send)
		{
			try
			{
				invitationManagement.sendInvitation(code);
			} catch (EngineException e)
			{
				String info = msg.getMessage("InvitationsTable.errorSend");
				NotificationPopup.showError(msg, info, e);
			}
		}
		return true;
	}

	private void removeInvitation(Set<TableInvitationBean> items)
	{
		try
		{
			for (TableInvitationBean item: items)
			{
				invitationManagement.removeInvitation(item.getCode());
			}
			refresh();
		} catch (Exception e)
		{
			String info = msg.getMessage("InvitationsTable.errorDelete");
			NotificationPopup.showError(msg, info, e);
		}
	}


	private SingleActionHandler<TableInvitationBean> getSendAction()
	{
		return SingleActionHandler.builder(TableInvitationBean.class)
			.withCaption(msg.getMessage("InvitationsTable.sendCodeAction"))
			.withIcon(Images.messageSend.getResource())
			.multiTarget()
			.withHandler(this::sendInvitation)
			.build();
	}
	
	private void sendInvitation(Set<TableInvitationBean> items)
	{
		try
		{
			for (TableInvitationBean item: items)
			{
				invitationManagement.sendInvitation(item.getCode());
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
	
	private SingleActionHandler<TableInvitationBean> getRefreshAction()
	{
		return SingleActionHandler
			.builder4Refresh(msg, TableInvitationBean.class)
			.withHandler(selection -> refresh())
			.build();
	}
	
	private void refresh()
	{
		try
		{
			TableInvitationBean selected = getOnlyOneSelected();
			List<TableInvitationBean> invitations = invitationManagement.getInvitations()
					.stream()
					.map(invitation -> new TableInvitationBean(invitation))
					.collect(Collectors.toList());
			invitationsTable.setItems(invitations);
			if (selected != null)
			{
				String selectedCode = selected.getCode();
				invitations.stream()
					.filter(invitation -> selectedCode.equals(invitation.getCode()))
					.findFirst()
					.ifPresent(invitation -> invitationsTable.select(invitation));
			}
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("InvitationsTable.errorGetInvitations"), e);
			setCompositionRoot(error);
		}
	}
	
	private SingleActionHandler<TableInvitationBean> getAddAction()
	{
		return SingleActionHandler.builder(TableInvitationBean.class)
			.withCaption(msg.getMessage("InvitationsTable.addInvitationAction"))
			.withIcon(Images.add.getResource())
			.dontRequireTarget()
			.withHandler(this::handleAdd)
			.build();
	}

	private void handleAdd(Set<TableInvitationBean> items)
	{
		InvitationEditor editor;
		try
		{
			editor = new InvitationEditor(msg, identityEditorRegistry,
					attrHandlersRegistry, msgTemplateManagement.listTemplates(),
					getForms(), attributesManagement.getAttributeTypesAsMap());
		} catch (WrongArgumentException e)
		{
			NotificationPopup.showError(msg.getMessage("InvitationsTable.noValidForms"), 
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
	
	private SingleActionHandler<TableInvitationBean> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, TableInvitationBean.class)
			.withHandler(this::handleDelete)
			.build();
	}
	
	public void handleDelete(Set<TableInvitationBean> items)
	{
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
		
		public String getExpiration()
		{
			return TimeUtil.formatMediumInstant(invitation.getExpiration());
		}
		
		public boolean isExpired()
		{
			return Instant.now().isAfter(invitation.getExpiration());
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
