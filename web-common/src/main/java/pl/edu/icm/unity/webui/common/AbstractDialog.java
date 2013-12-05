/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Iterator;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


/**
 * Base for the dialog windows. Provides boilerplate code. In particular the overall layout is created
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
	private Button enterButton;
	private Button escapeButton;
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
	
	/**
	 * Element returned by this method gets an initial focus.
	 * 
	 * @return by default returns the first {@link AbstractField} which is found in the dialog contents
	 * or null if none is found. Override to set the initial focus on other element.
	 */
	protected AbstractField<?> getFocussedComponent()
	{
		return getFocussedComponentRec(contentsComponent);
	}

	private AbstractField<?> getFocussedComponentRec(Component c)
	{
		if (!(c instanceof ComponentContainer))
			return null;
		ComponentContainer container = (ComponentContainer) c;
		Iterator<Component> components = container.iterator();
		while(components.hasNext())
		{
			Component cc = components.next();
			if (cc instanceof AbstractField)
				return (AbstractField<?>) cc;
			if (cc instanceof ComponentContainer)
				return getFocussedComponentRec(cc);
		}
		return null;
	}
	
	/**
	 * @return a button that is bound to the enter key or null if no button should be bound to Enter. By default
	 * the confirm button is returned.
	 */
	protected Button getDefaultOKButton()
	{
		return (getDefaultCancelButton()==confirm) ? null : confirm;
	}
	
	/**
	 * @return a button that is bound to the Escape key or null if no button should be bound. By default
	 * the cancel button is returned if present, if not - then the confirm button 
	 * (which is typically the 'close' button then).
	 */
	protected Button getDefaultCancelButton()
	{
		return cancel == null ? confirm : cancel;
	}
	
	
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
		
		enterButton = getDefaultOKButton();
		if (enterButton != null)
			enterButton.setClickShortcut(KeyCode.ENTER);
		escapeButton = getDefaultCancelButton();
		if (escapeButton != null)
			escapeButton.setClickShortcut(KeyCode.ESCAPE);
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
		focus();
		AbstractField<?> toFocus = getFocussedComponent();
		if (toFocus != null)
			toFocus.focus();
	}
	
	public void show()
	{
		show(defaultSizeUndfined);
	}
	
	public void close()
	{
		if (enterButton != null)
			enterButton.removeClickShortcut();
		if (escapeButton != null)
			escapeButton.removeClickShortcut();
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