/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.messagetemplates;

import pl.edu.icm.unity.notifications.MessageTemplate;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;



/**
 * Responsible for message templates management
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
		setWidth(50, Unit.PERCENTAGE);
		setHeight(55, Unit.PERCENTAGE);
	}

	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout vl = new VerticalLayout();
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_LEFT);
		vl.setHeight(100, Unit.PERCENTAGE);
		return vl;
	}
	@Override
	protected void onConfirm()
	{
		MessageTemplate template = editor.getTemplate();
		if (callback.newTemplate(template))
			close();
	}
	
	public interface Callback
	{
		public boolean newTemplate(MessageTemplate template);
	}
	
}