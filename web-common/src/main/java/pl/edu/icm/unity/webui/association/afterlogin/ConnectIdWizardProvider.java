/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.afterlogin;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.ui.UI;

import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.webui.association.IntroStep;
import pl.edu.icm.unity.webui.association.SandboxStep;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier.AuthnResultListener;
import pl.edu.icm.unity.webui.sandbox.wizard.AbstractSandboxWizardProvider;

/**
 * Configures account association wizard.
 * @author K. Benedyczak
 */
public class ConnectIdWizardProvider extends AbstractSandboxWizardProvider
{
	private UnityMessageSource msg;
	private InputTranslationEngine translationEngine;
	private WizardFinishedCallback callback;
	private MergeCurrentWithUnknownConfirmationStep confirmationStep;

	public ConnectIdWizardProvider(UnityMessageSource msg, String sandboxURL, SandboxAuthnNotifier sandboxNotifier,
			InputTranslationEngine translationEngine, WizardFinishedCallback callback)
	{
		super(sandboxURL, sandboxNotifier);
		this.msg = msg;
		this.translationEngine = translationEngine;
		this.callback = callback;
	}

	@Override
	public Wizard getWizardInstance()
	{
		final Wizard wizard = new Wizard();
		wizard.getNextButton().setId("SandboxWizard.next");
		wizard.getFinishButton().setId("SandboxWizard.finish");
		wizard.setSizeFull();
		wizard.addStep(new IntroStep(msg, "ConnectId.introLabel"));
		final SandboxStep sandboxStep = new SandboxStep(msg, sandboxURL, wizard);
		wizard.addStep(sandboxStep);
		confirmationStep = new MergeCurrentWithUnknownConfirmationStep(msg, translationEngine, wizard);
		wizard.addStep(confirmationStep);
		
		openSandboxPopupOnNextButton(wizard);
		showSandboxPopupAfterGivenStep(wizard, IntroStep.class);
		addSandboxListener(new AuthnResultListener()
		{
			@Override
			public void onPartialAuthnResult(SandboxAuthnEvent event)
			{
				sandboxStep.enableNext();
				confirmationStep.setAuthnData(event);
				wizard.next();						
			}

			@Override
			public void onCompleteAuthnResult(AuthenticatedEntity authenticatedEntity)
			{
			}
		}, wizard, UI.getCurrent(), false);
		return wizard;
	}

	@Override
	protected void showSandboxPopupAfterGivenStep(final Wizard wizard, final Class<?> prePopupStepClass)
	{
		wizard.addListener(new WizardProgressListener()
		{
			@Override
			public void wizardCompleted(WizardCompletedEvent event)	
			{
				
				if (confirmationStep.getMergeError() != null)
					callback.onError(confirmationStep.getMergeError());
				else
					callback.onSuccess(confirmationStep.getMerged());
			}
			
			@Override
			public void wizardCancelled(WizardCancelledEvent event)	
			{
				callback.onCancel();
			}
			
			@Override
			public void stepSetChanged(WizardStepSetChangedEvent event) {}
			
			@Override
			public void activeStepChanged(WizardStepActivationEvent event)
			{
				if (event.getActivatedStep().getClass().isAssignableFrom(prePopupStepClass)) 
					openSandboxPopupOnNextButton(wizard);
			}
		});
	}
	
	public interface WizardFinishedCallback
	{
		void onSuccess(MappingResult mergedIdentity);
		void onError(Exception error);
		void onCancel();
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("ConnectId.wizardCaption");
	}
}
