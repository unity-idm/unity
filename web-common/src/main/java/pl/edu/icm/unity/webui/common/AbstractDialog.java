/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Iterator;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import pl.edu.icm.unity.webui.common.Styles;
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
	public enum SizeMode {LARGE, SMALL}
	
	private static final long serialVersionUID = 1L;
	private Button confirm;
	private Button cancel;
	private Button enterButton;
	private Button escapeButton;
	protected Component contentsComponent;
	protected UnityMessageSource msg;
	protected boolean lightweightWrapperPanel = false;
	private int width = 50;
	private int height = 50;
	private SizeMode sizeMode = SizeMode.LARGE; 
	
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
		confirm.setId("AbstractDialog.confirm");
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
	
	protected void setSize(int widthPercentage, int heightPercentage)
	{
		this.width = widthPercentage;
		this.height = heightPercentage;
	}
	
	/**
	 * @param sizeMode if LARGE (what is default) then the dialog size is set to 80x90%, both scrollbars are 
	 * provided if the content is too big to fit the viewport. If false, then the size is set to 50%x60%
	 *  only vertical scrollbar will be displayed, and the contents is centered horizontally (assuming it has
	 *  no 100% width by itself. In any case the initial sizes can be further changed with 
	 *  {@link #setSize(int, int)}.
	 *  
	 */
	protected void setSizeMode(SizeMode sizeMode)
	{
		this.sizeMode = sizeMode;
		if (sizeMode == SizeMode.LARGE)
			setSize(80, 90);
		else
			setSize(50, 60);
	}
	
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
			{
				if (!((AbstractField<?>)cc).isReadOnly())
					return (AbstractField<?>) cc;
			}
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
	
	
	
	protected void initGUI() throws Exception
	{
		setModal(true);
		setClosable(false);

		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		vl.setMargin(true);
		vl.setSizeFull();
		
		Panel contentsPanel = new SafePanel();
		contentsPanel.setSizeFull();
		if (lightweightWrapperPanel)
			contentsPanel.addStyleName(Styles.vPanelLight.toString());
		VerticalLayout internal = new VerticalLayout();
		contentsComponent = getContents();
		internal.addComponent(contentsComponent);
		internal.setComponentAlignment(contentsComponent, Alignment.MIDDLE_CENTER);
		internal.setMargin(true);
		internal.setSizeUndefined();
		contentsPanel.setContent(internal);
		if (sizeMode == SizeMode.SMALL)
			contentsPanel.addStyleName(Styles.centeredPanel.toString());
		vl.addComponent(contentsPanel);
		
		Layout hl = getButtonsBar();
		vl.addComponent(hl);
		vl.setComponentAlignment(hl, Alignment.BOTTOM_RIGHT);
		
		vl.setExpandRatio(contentsPanel, 4.0f);
		setContent(vl);
		setWidth(width, Unit.PERCENTAGE);
		setHeight(height, Unit.PERCENTAGE);
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
	
	public void show()
	{
		try
		{
			initGUI();
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
	
	public void close()
	{
		unbindEnterShortcut();
		if (escapeButton != null)
			escapeButton.removeClickShortcut();
		if (getParent() != null)
			((UI) getParent()).removeWindow(this);
	}
	
	public void unbindEnterShortcut()
	{
		if (enterButton != null)
			enterButton.removeClickShortcut();	
	}
	
	public void buttonClick(ClickEvent event) {
		if (event.getSource() == confirm)
			onConfirm();
		if (event.getSource() == cancel)
			onCancel();
	}
}