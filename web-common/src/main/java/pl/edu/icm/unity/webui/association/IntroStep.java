/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Shows introduction with information on the association feature.
 * @author K. Benedyczak
 */
public class IntroStep extends CustomComponent implements WizardStep
{
	private UnityMessageSource msg;
	private HtmlLabel introLabel;
	
	public IntroStep(UnityMessageSource msg, String introTextKey)
	{
		this.msg = msg;
		setCompositionRoot(buildMainLayout(introTextKey));
	}

	private VerticalLayout buildMainLayout(String introTextKey) 
	{
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setSpacing(false);
		setSizeFull();
		
		introLabel = new HtmlLabel(msg);
		introLabel.setHtmlValue(introTextKey);
		introLabel.setWidth(100, Unit.PERCENTAGE);
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
		return true;
	}

	@Override
	public boolean onBack()
	{
		return false;
	}
}
