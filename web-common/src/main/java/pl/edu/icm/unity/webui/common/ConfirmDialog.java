/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

/**
 * Confirmation dialog
 * @author K. Benedyczak
 */
public class ConfirmDialog extends AbstractDialog
{
	private static final long serialVersionUID = 1L;
	private Callback callback;
	private String question;

	public ConfirmDialog(UnityMessageSource msg, String question, Callback callback) 
	{
		super(msg, msg.getMessage("ConfirmDialog.confirm"));
		this.question = question;
		this.callback = callback;
	}
	
	public ConfirmDialog(UnityMessageSource msg, String caption, String question, 
			Callback callback) 
	{
		super(msg, caption);
		this.question = question;
		this.callback = callback;
	}

	public interface Callback 
	{
		public void onConfirm();
	}

	@Override
	protected Component getContents()
	{
		return new Label(question);
	}

	@Override
	protected void onConfirm()
	{
		close();
		callback.onConfirm();
	}
}