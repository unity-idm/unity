/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.connectid;

import org.vaadin.teemu.wizards.WizardStep;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

/**
 * Shows confirmation of the account association
 * @author K. Benedyczak
 */
public class ConfirmationStep extends CustomComponent implements WizardStep
{
	private UnityMessageSource msg;
	private HtmlLabel introLabel;
	
	public ConfirmationStep(UnityMessageSource msg)
	{
		this.msg = msg;
		setCompositionRoot(buildMainLayout());
	}

	private VerticalLayout buildMainLayout() 
	{
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setMargin(true);
		setSizeFull();
		
		introLabel = new HtmlLabel(msg);
		introLabel.setHtmlValue("ConnectId.introLabel");
		introLabel.setImmediate(false);
		mainLayout.addComponent(introLabel);
		
		return mainLayout;
	}
	
	@Override
	public String getCaption()
	{
		return msg.getMessage("Wizard.IntroStep.caption");
	}

	@Override
	public Component getContent()
	{
		return this;
	}

	@Override
	public boolean onAdvance()
	{
		return false;
	}

	@Override
	public boolean onBack()
	{
		return true;
	}
}
