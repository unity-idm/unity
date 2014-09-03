/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Dialog window that deals with profile wizard.
 * 
 * @author Roman Krysinski
 */
public class WizardDialog extends Window 
{
	private static final long serialVersionUID = -7774178147989237210L;

	public WizardDialog(UnityMessageSource msg)
	{
		setModal(true);
		setClosable(false);
		setWidth(80, Unit.PERCENTAGE);
		setHeight(85, Unit.PERCENTAGE);
		setContent(new WizardDialogComponent(msg));
	}
	
	public void show()
	{
		UI.getCurrent().addWindow(this);
		focus();
	}
	
	public void close()
	{
		if (getParent() != null)
		{
			((UI) getParent()).removeWindow(this);
		}
	}	
	
}
