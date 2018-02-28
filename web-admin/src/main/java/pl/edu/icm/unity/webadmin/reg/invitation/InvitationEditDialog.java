/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Dialog for {@link InvitationEditor}
 * @author Krzysztof Benedyczak
 */
public class InvitationEditDialog extends AbstractDialog
{
	private InvitationEditor editor;
	private Callback callback;
	private Button createAndSend;
	
	public InvitationEditDialog(UnityMessageSource msg, String caption, 
			InvitationEditor editor, Callback callback)
	{
		super(msg, caption, msg.getMessage("InvitationEditDialog.createInvitation"), 
				msg.getMessage("cancel"));
		this.editor = editor;
		this.callback = callback;
		setSizeMode(SizeMode.LARGE);
		createAndSend = new Button(msg.getMessage("InvitationEditDialog.createAndSend"), this);
	}
	
	@Override
	protected AbstractOrderedLayout getButtonsBar()
	{
		AbstractOrderedLayout ret = super.getButtonsBar();
		ret.addComponent(createAndSend, 0);
		return ret;
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getSource() == createAndSend)
			onConfirm(true);
		super.buttonClick(event);
	}
	
	@Override
	protected Component getContents() throws Exception
	{
		return editor;
	}
	
	protected void onConfirm(boolean send)
	{
		try
		{
			InvitationParam invitation = editor.getInvitation();
			if (send && (invitation.getContactAddress() == null || 
					invitation.getContactAddress().isEmpty()))
				throw new FormValidationException(msg.getMessage("InvitationEditDialog.addressMandatoryToSend"));
			if (callback.onInvitation(invitation, send))
				close();
		} catch (FormValidationException e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
	}

	
	@Override
	protected void onConfirm()
	{
		onConfirm(false);
	}

	public interface Callback
	{
		public boolean onInvitation(InvitationParam invitation, boolean sendInvitation);
	}
}
