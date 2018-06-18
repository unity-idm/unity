/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Notification;

import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.SingleCredentialChangeDialog;

/**
 * Simple dialog wrapping {@link SingleCredentialChangeDialog}. It is invoked for users logged with outdated
 * credential. User is informed about invalidated credential. After changing the credential user can only logout.
 *   
 * @author K. Benedyczak
 */
@PrototypeComponent
public class OutdatedCredentialDialog 
{
	private StandardWebAuthenticationProcessor authnProcessor;
	private ObjectFactory<SingleCredentialChangeDialog> credChangeDialogFactory;
	private UnityMessageSource msg;
	
	@Autowired
	public OutdatedCredentialDialog(UnityMessageSource msg, 
			ObjectFactory<SingleCredentialChangeDialog> credChangeDialogFactory)
	{
		this.msg = msg;
		this.credChangeDialogFactory = credChangeDialogFactory;
	}

	public void show(StandardWebAuthenticationProcessor authnProcessor)
	{
		this.authnProcessor = authnProcessor;
		Notification notification = NotificationPopup.getNoticeNotification(
				msg.getMessage("OutdatedCredentialDialog.caption"), 
				msg.getMessage("OutdatedCredentialDialog.info"));
		notification.addCloseListener(e -> onConfirm());
		notification.show(Page.getCurrent());
	}
	
	private void onConfirm()
	{
		WrappedSession vss = VaadinSession.getCurrent().getSession();
		LoginSession ls = (LoginSession) vss.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY);
		SingleCredentialChangeDialog dialog = credChangeDialogFactory.getObject().init(ls.getEntityId(), 
				true,
				changed -> afterCredentialUpdate(changed), 
				ls.getOutdatedCredentialId());
		dialog.show();
	}

	private void afterCredentialUpdate(final boolean changed)
	{
		authnProcessor.logout(true);
		if (changed)
		{
			NotificationPopup.showSuccess(msg.getMessage("OutdatedCredentialDialog.finalCaption"), 
					msg.getMessage("OutdatedCredentialDialog.finalInfo"));
		} else
		{
			NotificationPopup.showError(msg.getMessage("OutdatedCredentialDialog.finalCaption"), 
					msg.getMessage("OutdatedCredentialDialog.finalInfoNotChanged"));
		}
	}
}
