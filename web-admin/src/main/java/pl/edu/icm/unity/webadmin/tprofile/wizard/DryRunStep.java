/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import org.vaadin.teemu.wizards.WizardStep;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Component;

/**
 * Fourth wizard step with profile validation - used in {@link WizardDialogComponent}.
 * 
 * @author Roman Krysinski
 */
public class DryRunStep implements WizardStep 
{

	private UnityMessageSource msg;

	public DryRunStep(UnityMessageSource msg) 
	{
		this.msg = msg;
	}

	@Override
	public String getCaption() 
	{
		return msg.getMessage("Wizard.DryRunStep.caption");
	}

	@Override
	public Component getContent() 
	{
		return new DryRunStepComponent(msg);
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
