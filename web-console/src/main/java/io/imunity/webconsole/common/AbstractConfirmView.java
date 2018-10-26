/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.common;

import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Base for the simple confirm view. Provides boilerplate code. In particular
 * the overall layout is created with two buttons at the bottom: one responsible
 * for confirm and another for cancel.
 * 
 * @author P.Piernik
 *
 */
public abstract class AbstractConfirmView extends VerticalLayout
		implements Button.ClickListener, View
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			AbstractConfirmView.class);

	protected Button confirm;
	protected Button cancel;
	protected Component contentsComponent;
	protected UnityMessageSource msg;

	public AbstractConfirmView(UnityMessageSource msg, String confirmM)
	{
		this(msg, confirmM, null);
	}

	public AbstractConfirmView(UnityMessageSource msg, String confirmM, String cancelM)
	{
		this.msg = msg;
		if (cancelM != null)
			cancel = createCancelButton();
		confirm = createConfirmButton();
	}

	protected Button createConfirmButton()
	{
		Button confirm = new Button(msg.getMessage("ok"), this);
		confirm.setId("AbstractDialog.confirm");
		confirm.addStyleName("u-dialog-confirm");
		return confirm;
	}

	protected Button createCancelButton()
	{
		Button confirm = new Button(msg.getMessage("cancel"), this);
		confirm.addStyleName("u-dialog-cancel");
		return confirm;
	}

	protected AbstractOrderedLayout getButtonsBar()
	{
		HorizontalLayout hl = new HorizontalLayout();
		if (cancel != null)
			hl.addComponent(cancel);
		hl.addComponent(confirm);
		return hl;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		try
		{
			contentsComponent = getContents(event.getParameterMap());
		} catch (Exception e)
		{
			log.error("Error when view init", e);
			if (e instanceof RuntimeException)
				throw ((RuntimeException) e);
			return;
		}

		addComponent(contentsComponent);
		Layout hl = getButtonsBar();
		addComponent(hl);

	}

	protected abstract Component getContents(Map<String, String> map) throws Exception;

	protected abstract void onConfirm();

	protected abstract void onCancel();

	@Override
	public void buttonClick(ClickEvent event)
	{
		if (event.getSource() == confirm)
			onConfirm();
		if (event.getSource() == cancel)
			onCancel();
	}
}
