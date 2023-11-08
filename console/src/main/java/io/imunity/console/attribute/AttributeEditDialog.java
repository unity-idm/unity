/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.attribute;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;


public class AttributeEditDialog extends ConfirmDialog
{
	private final AttributeEditor editor;
	private final Callback callback;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	public AttributeEditDialog(MessageSource msg, String caption, Callback callback, 
			AttributeEditor attributeEditor, NotificationPresenter notificationPresenter)
	{
		setHeader(caption);
		this.editor = attributeEditor;
		this.callback = callback;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		setCancelable(true);
		setConfirmButton(msg.getMessage("ok"), event -> onConfirm());
		setWidth("64em");
		setHeight("30em");
		add(editor);
	}

	protected void onConfirm()
	{
		Attribute attribute;
		try
		{
			attribute = editor.getAttribute();
			if (callback.newAttribute(attribute))
				close();
		} catch (FormValidationException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
		}
	}
	
	public interface Callback
	{
		boolean newAttribute(Attribute newAttribute);
	}
}
