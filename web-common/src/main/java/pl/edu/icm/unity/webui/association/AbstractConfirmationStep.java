/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import pl.edu.icm.unity.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.server.authn.LocalSandboxAuthnContext;
import pl.edu.icm.unity.server.authn.SandboxAuthnContext;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

/**
 * Shows confirmation of the account association and invokes the operation. 
 * This class is base class. Extensions can work in one of two modes: either to require
 * to get an unknown user from sandbox login or the get an existing user from sandbox login.
 * Then the merge is performed in a proper way, depending on context.
 * @author K. Benedyczak
 */
public abstract class AbstractConfirmationStep extends CustomComponent implements WizardStep
{
	protected UnityMessageSource msg;
	protected HtmlLabel introLabel;
	protected InputTranslationEngine translationEngine;
	protected ErrorComponent errorComponent;
	
	public AbstractConfirmationStep(UnityMessageSource msg, InputTranslationEngine translationEngine, 
			final Wizard wizard)
	{
		this.msg = msg;
		this.translationEngine = translationEngine;
		setCompositionRoot(buildMainLayout());
		wizard.addListener(new WizardProgressListener()
		{
			@Override
			public void wizardCompleted(WizardCompletedEvent event)
			{
				merge();
			}
			
			@Override
			public void wizardCancelled(WizardCancelledEvent event)
			{
			}
			
			@Override
			public void stepSetChanged(WizardStepSetChangedEvent event)
			{
			}
			
			@Override
			public void activeStepChanged(WizardStepActivationEvent event)
			{
				if (event.getActivatedStep() instanceof AbstractConfirmationStep)
				{
					wizard.getBackButton().setEnabled(false);
					if (errorComponent.isVisible())
						wizard.getFinishButton().setEnabled(false);
				}
			}
		});
	}

	/**
	 * Performs the merge assuming that the all the settings are all right.
	 */
	protected abstract void merge();

	/**
	 * invoked when sandbox returns a remotely authenticated user 
	 * @param ctx
	 */
	protected abstract void setRemoteAuthnData(RemoteSandboxAuthnContext ctx);

	/**
	 * invoked when sandbox callback returns a locally authenticated user.
	 * @param ctx
	 */
	protected abstract void setLocalAuthnData(LocalSandboxAuthnContext ctx);


	
	private VerticalLayout buildMainLayout() 
	{
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setMargin(true);
		setSizeFull();
		
		introLabel = new HtmlLabel(msg);
		mainLayout.addComponent(introLabel);
		
		errorComponent = new ErrorComponent();
		mainLayout.addComponent(errorComponent);
		errorComponent.setVisible(false);
		
		return mainLayout;
	}

	public void setAuthnData(SandboxAuthnEvent event)
	{
		SandboxAuthnContext ctx = event.getCtx();
		if (ctx instanceof RemoteSandboxAuthnContext)
			setRemoteAuthnData((RemoteSandboxAuthnContext) ctx);
		else
			setLocalAuthnData((LocalSandboxAuthnContext) ctx);
	}
	
	protected void setError(String message)
	{
		introLabel.setVisible(false);
		errorComponent.setVisible(true);
		errorComponent.setError(message);
	}
	
	@Override
	public String getCaption()
	{
		return msg.getMessage("ConnectId.ConfirmStep.caption");
	}

	@Override
	public Component getContent()
	{
		return this;
	}

	@Override
	public boolean onBack()
	{
		return false;
	}
}
