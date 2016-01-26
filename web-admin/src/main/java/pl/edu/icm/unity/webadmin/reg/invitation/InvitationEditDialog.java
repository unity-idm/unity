/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.webadmin.reg.invitation;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.ui.Component;

/**
 * Dialog for {@link InvitationEditor}
 * @author Krzysztof Benedyczak
 */
public class InvitationEditDialog extends AbstractDialog
{
	private InvitationEditor editor;
	private Callback callback;
	
	public InvitationEditDialog(UnityMessageSource msg, String caption, 
			InvitationEditor editor, Callback callback)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
		setSizeMode(SizeMode.LARGE);
	}

	@Override
	protected Component getContents() throws Exception
	{
		return editor;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			InvitationParam invitation = editor.getInvitation();
			if (callback.onInvitation(invitation))
				close();
		} catch (FormValidationException e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
	}

	public interface Callback
	{
		public boolean onInvitation(InvitationParam invitation);
	}
}
