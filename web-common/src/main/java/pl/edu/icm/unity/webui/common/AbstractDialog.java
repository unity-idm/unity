/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


/**
 * Base for the dialog windows. Provides boilerplate code. In particular the overal layout is created
 * with two buttons at the bottom: one responsible for confirm and another for cancel.
 * <p>
 * Usage pattern: extend, provide onConfirm() logic and custom contents in getContents(). Then call show();
 * Note: the returned contents component is available in the field.
 * Note: the initialization of the GUI is done in the show() call, not in the constructor.
 * 
 * @author K. Benedyczak
 */
public abstract class AbstractDialog extends Window implements Button.ClickListener 
{
	private static final long serialVersionUID = 1L;
	private Button confirm;
	private Button cancel;
	protected Component contentsComponent;
	protected UnityMessageSource msg;
	protected boolean defaultSizeUndfined = false;

	/**
	 * With only one, confirm button, which usually should be labelled as 'close'. 
	 * @param msg
	 * @param caption
	 * @param confirmM
	 */
	public AbstractDialog(UnityMessageSource msg, String caption, String confirmM) 
	{
		this(msg, caption, confirmM, null);
	}

	public AbstractDialog(UnityMessageSource msg, String caption, String confirmM, String cancelM) 
	{
		super(caption);
		this.msg = msg;
		confirm = new Button(confirmM, this);
		if (cancelM != null)
			cancel = new Button(cancelM, this);		
	}
	
	/**
	 * Standard version with OK and cancel.
	 * @param msg
	 * @param caption
	 */
	public AbstractDialog(UnityMessageSource msg, String caption) 
	{
		super(caption);
		this.msg = msg;
		confirm = new Button(msg.getMessage("ok"), this);
		cancel = new Button(msg.getMessage("cancel"), this);
	}	
	
	protected abstract Component getContents() throws Exception;
	protected abstract void onConfirm();
	
	protected void onCancel()
	{
		close();
	}
	
	protected void initGUI(boolean sizeUndefined) throws Exception
	{
		setModal(true);
		setClosable(false);

		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		vl.setMargin(true);
		
		Panel contentsPanel = new Panel();
		VerticalLayout internal = new VerticalLayout();
		contentsComponent = getContents();
		internal.addComponent(contentsComponent);
		internal.setComponentAlignment(contentsComponent, Alignment.MIDDLE_CENTER);
		if (!sizeUndefined)
			internal.setSizeFull();
		internal.setExpandRatio(contentsComponent, 1.0f);
		internal.setMargin(true);
		
		contentsPanel.setContent(internal);
		if (!sizeUndefined)
			contentsPanel.setSizeFull();
		vl.addComponent(contentsPanel);
		
		Layout hl = getButtonsBar();
		vl.addComponent(hl);
		vl.setComponentAlignment(hl, Alignment.BOTTOM_RIGHT);
		
		vl.setExpandRatio(contentsPanel, 4.0f);
		if (!sizeUndefined)
			vl.setSizeFull();
		setContent(vl);
	}
	
	protected AbstractOrderedLayout getButtonsBar()
	{
		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		hl.setMargin(true);
		hl.addComponent(confirm);
		if (cancel != null)
			hl.addComponent(cancel);
		return hl;
	}
	
	public void show(boolean sizeUndefined)
	{
		try
		{
			initGUI(sizeUndefined);
		} catch (Exception e)
		{
			if (e instanceof RuntimeException)
				throw ((RuntimeException)e);
			return;
		}
		UI.getCurrent().addWindow(this);
	}
	
	public void show()
	{
		show(defaultSizeUndfined);
	}
	
	public void close()
	{
		if (getParent() != null)
			((UI) getParent()).removeWindow(this);
	}
	
	public void buttonClick(ClickEvent event) {
		if (event.getSource() == confirm)
			onConfirm();
		if (event.getSource() == cancel)
			onCancel();
	}
}