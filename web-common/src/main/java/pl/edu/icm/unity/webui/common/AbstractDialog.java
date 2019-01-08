/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Iterator;

import org.apache.logging.log4j.Logger;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;


/**
 * Base for the dialog windows. Provides boilerplate code. In particular the overall layout is created
 * with two buttons at the bottom: one responsible for confirm and another for cancel.
 * <p>
 * Usage pattern: extend, provide onConfirm() logic and custom contents in getContents(). Then call show();
 * Note: the returned contents component is available in the field.
 * Note: the initialization of the GUI is done in the show() call, not in the constructor.
 * <p>
 * The contents will be displayed differently depending whether the returned component has width undefined/fixed 
 * or set as percentage. In the first case the component will be centered horizontally and scrollbar 
 * will be shown if it overflows. In the latter the component will be left aligned with the scrollbar shown when needed.
 * However in the latter case the long labels etc. will not be wrapped, as they are in the first case.  
 * @author K. Benedyczak
 */
public abstract class AbstractDialog extends Window implements Button.ClickListener 
{
	//TODO remove percentage sizeing
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AbstractDialog.class);

	public enum SizeMode {LARGE, MEDIUM, SMALL}
	
	protected Button confirm;
	protected Button cancel;
	private Button enterButton;
	private Button escapeButton;
	protected Component contentsComponent;
	protected UnityMessageSource msg;
	protected boolean lightweightWrapperPanel = false;
	private int width = 50;
	private int height = 50;
	private float widthEm;
	private float heightEm;
	private String confirmMessage;
	private String cancelMessage;
	
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
		this.cancelMessage = cancelM;
		this.confirmMessage = confirmM;
		if (cancelM != null)
			cancel = createCancelButton();
		confirm = createConfirmButton();
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
		cancel = createCancelButton();
		confirm = createConfirmButton();
	}	
	
	protected abstract Component getContents() throws Exception;
	protected abstract void onConfirm();
	
	protected void setSize(int widthPercentage, int heightPercentage)
	{
		this.width = widthPercentage;
		this.height = heightPercentage;
	}
	
	protected void setSizeEm(float widthEm, float heightEm)
	{
		this.widthEm = widthEm;
		this.heightEm = heightEm;
	}
	
	protected Button createConfirmButton()
	{
		Button confirm = new Button(confirmMessage == null ? msg.getMessage("ok") : confirmMessage, this);
		confirm.setId("AbstractDialog.confirm");
		confirm.addStyleName("u-dialog-confirm");
		return confirm;
	}

	protected Button createCancelButton()
	{
		Button confirm = new Button(cancelMessage == null ? msg.getMessage("cancel") : cancelMessage, this);
		confirm.addStyleName("u-dialog-cancel");
		return confirm;
	}
	
	/**
	 * Quickly set typical sizes of the dialog window.
	 * @param sizeMode 
	 *  
	 */
	protected void setSizeMode(SizeMode sizeMode)
	{
		switch (sizeMode)
		{
		case LARGE:
			setSize(80, 90);
			break;
		case MEDIUM:
			setSize(50, 60);
			break;
		case SMALL:
			setSizeEm(32, 16);
			break;
		default:
			break;
		
		}
	}
	
	/**
	 * Element returned by this method gets an initial focus.
	 * 
	 * @return by default returns the first {@link AbstractField} which is found in the dialog contents
	 * or null if none is found. Override to set the initial focus on other element.
	 */
	protected Focusable getFocussedComponent()
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
		vl.setSizeFull();
		vl.addStyleName("u-dialog-contents");
		
		Panel contentsPanel = new SafePanel();
		contentsPanel.setSizeFull();
		if (lightweightWrapperPanel)
			contentsPanel.addStyleName(Styles.vPanelLight.toString());
		VerticalLayout internal = new VerticalLayout();
		contentsComponent = getContents();
		internal.addComponent(contentsComponent);
		internal.setComponentAlignment(contentsComponent, Alignment.MIDDLE_CENTER);
		internal.setSpacing(false);
		internal.addStyleName(Styles.visibleScroll.toString());
		contentsPanel.setContent(internal);
		contentsPanel.addStyleName(Styles.centeredPanel.toString());
		vl.addComponent(contentsPanel);
		
		Layout hl = getButtonsBar();
		vl.addComponent(hl);
		vl.setComponentAlignment(hl, Alignment.BOTTOM_RIGHT);
		
		vl.setExpandRatio(contentsPanel, 4.0f);
		setContent(vl);
		
		if (widthEm == 0)
		{
			setWidth(width, Unit.PERCENTAGE);
			setHeight(height, Unit.PERCENTAGE);
		} else
		{
			setWidth(widthEm, Unit.EM);
			setHeight(heightEm, Unit.EM);
		}
		enterButton = getDefaultOKButton();
		if (enterButton != null)
		{
			enterButton.setClickShortcut(KeyCode.ENTER);
			enterButton.addStyleName(Styles.vButtonPrimary.toString());
		}
		escapeButton = getDefaultCancelButton();
		if (escapeButton != null)
			escapeButton.setClickShortcut(KeyCode.ESCAPE);
	}
	
	protected AbstractOrderedLayout getButtonsBar()
	{
		HorizontalLayout hl = new HorizontalLayout();
		if (cancel != null)
			hl.addComponent(cancel);
		hl.addComponent(confirm);
		return hl;
	}
	
	public void show()
	{
		try
		{
			initGUI();
		} catch (Exception e)
		{
			log.error("Error when dialog init", e);
			if (e instanceof RuntimeException)
				throw ((RuntimeException)e);
			return;
		}
		UI.getCurrent().addWindow(this);
		focus();
		Focusable toFocus = getFocussedComponent();
		if (toFocus != null)
			toFocus.focus();
	}
	
	@Override
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
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getSource() == confirm)
			onConfirm();
		if (event.getSource() == cancel)
			onCancel();
	}
}