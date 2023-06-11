/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webelements.clipboard;

import com.vaadin.jsclipboard.JSClipboard;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationTray;
import pl.edu.icm.unity.webui.common.Styles;

public class CopyToClipboardButton extends CustomComponent
{
	public CopyToClipboardButton(MessageSource msg, TextField field)
	{
		Button copy = new Button();
		copy.setDescription(msg.getMessage("CopyToClipboardButton.copyToClipboard"));
		copy.setIcon(Images.copy.getResource());
		copy.setStyleName(Styles.vButtonLink.toString());
		copy.addStyleName(Styles.vButtonBorderless.toString());
		copy.addStyleName(Styles.link.toString());
		JSClipboard clipboard = new JSClipboard();
		clipboard.apply(copy, field);
		clipboard.addSuccessListener(() -> NotificationTray.showSuccess(
				msg.getMessage("CopyToClipboardButton.successCopiedToClipboard")));
		setCompositionRoot(copy);
	}
}
