/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.msgtemplate;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;



/**
 * Responsible for message template edit
 * @author P. Piernik
 *
 */
public class MessageTemplateEditDialog extends AbstractDialog
{
	MessageTemplateEditor editor;
	private Callback callback;
	public MessageTemplateEditDialog(UnityMessageSource msg, String caption,
			Callback callback, MessageTemplateEditor editor)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws Exception
	{
		setSizeMode(SizeMode.LARGE);
		VerticalLayout vl = new VerticalLayout();
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_LEFT);
		vl.setHeight(100, Unit.PERCENTAGE);
		vl.setSpacing(false);
		vl.setMargin(false);
		return vl;
	}
	@Override
	protected void onConfirm()
	{
		MessageTemplate template = editor.getTemplate();
		if (template == null)
		{
			NotificationPopup.showFormError(msg);
			return;
		}
		if (callback.newTemplate(template))
			close();
	}
	
	@Override
	public void show()
	{
		super.show();
		unbindEnterShortcut();
	}
	
	public interface Callback
	{
		public boolean newTemplate(MessageTemplate template);
	}
	
}