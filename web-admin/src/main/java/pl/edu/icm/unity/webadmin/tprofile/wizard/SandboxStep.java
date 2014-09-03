/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import org.vaadin.teemu.wizards.WizardStep;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Component;

/**
 * Second wizard step with sandbox authentication to get input 
 * for profile creation- used in {@link WizardDialogComponent}.
 * 
 * @author Roman Krysinski
 */
public class SandboxStep implements WizardStep 
{

	public SandboxStep(UnityMessageSource msg) 
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getCaption() 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Component getContent() 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onAdvance() 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onBack() 
	{
		// TODO Auto-generated method stub
		return false;
	}

}
