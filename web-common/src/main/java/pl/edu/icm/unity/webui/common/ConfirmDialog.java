/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;

/**
 * Confirmation dialog
 * 
 * @author K. Benedyczak
 */
public class ConfirmDialog extends AbstractDialog
{
	private Callback callback;
	private Runnable cancelCallback;
	private String question;
	private boolean htmlContent = false;

	public ConfirmDialog(MessageSource msg, String question, Callback callback, Runnable cancelCallback)
	{
		this(msg, msg.getMessage("ConfirmDialog.confirm"), question, callback);
		this.cancelCallback = cancelCallback;
	}
	
	public ConfirmDialog(MessageSource msg, String caption, String question, String confirmM, String cancelM,
			Callback callback, Runnable cancelCallback)
	{
		this(msg, caption, question, confirmM, null, cancelM, null, callback);
		this.cancelCallback = cancelCallback;
	}
	
	
	public ConfirmDialog(MessageSource msg, String question, Callback callback)
	{
		this(msg, msg.getMessage("ConfirmDialog.confirm"), question, callback);
	}

	public ConfirmDialog(MessageSource msg, String question, String confirmM, String confirmT, String cancelM,
			String cancelT, Callback callback)
	{
		this(msg, msg.getMessage("ConfirmDialog.confirm"), question, confirmM, confirmT, cancelM, cancelT, callback);
	}
	
	public ConfirmDialog(MessageSource msg, String caption, String question, String confirmM, String confirmT, String cancelM,
			String cancelT, Callback callback)
	{
		super(msg, caption, confirmM, confirmT, cancelM, cancelT);
		this.question = question;
		this.callback = callback;
		this.lightweightWrapperPanel = true;
		setSizeMode(SizeMode.SMALL);
	}

	public ConfirmDialog(MessageSource msg, String caption, String question, Callback callback)
	{
		super(msg, caption);
		this.question = question;
		this.callback = callback;
		this.lightweightWrapperPanel = true;
		setSizeMode(SizeMode.SMALL);
	}

	protected ConfirmDialog(MessageSource msg, String question)
	{
		this(msg, msg.getMessage("ConfirmDialog.confirm"), question, null);
	}

	protected void setCallback(Callback callback)
	{
		this.callback = callback;
	}

	public void setSizeEm(float widthEm, float heightEm)
	{
		super.setSizeEm(widthEm, heightEm);
	}

	public void setHTMLContent(boolean how)
	{
		this.htmlContent = how;
	}

	public interface Callback
	{
		public void onConfirm();
	}

	@Override
	protected Component getContents()
	{
		Component ret = htmlContent ? new HtmlSimplifiedLabel(question) : new Label(question);
		ret.setWidth(100, Unit.PERCENTAGE);
		return ret;
	}

	@Override
	protected void onConfirm()
	{
		close();
		callback.onConfirm();
	}
	
	@Override
	protected void onCancel()
	{
		if (cancelCallback != null)
		{
			cancelCallback.run();
		}
		super.onCancel();
	}
}