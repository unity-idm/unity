/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.connectid;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
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
	private InputTranslationEngine translationEngine;
	private RemotelyAuthenticatedContext authnContext;
	private ErrorComponent errorComponent;
	
	public ConfirmationStep(UnityMessageSource msg, InputTranslationEngine translationEngine, Wizard wizard)
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
			}
		});
	}

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
		if (event.getCtx().getAuthnException() != null)
		{
			introLabel.setVisible(false);
			errorComponent.setVisible(true);
			errorComponent.setError(msg.getMessage("ConnectId.ConfirmStep.error"), 
					event.getCtx().getAuthnException());
		} else
		{
			authnContext = event.getCtx().getAuthnContext();
			introLabel.setHtmlValue("ConnectId.ConfirmStep.info", authnContext.getRemoteIdPName());
		}
	}
	
	private void merge()
	{
		if (authnContext == null)
			return;
			
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		try
		{
			translationEngine.mergeWithExisting(authnContext.getMappingResult(), 
					new EntityParam(loginSession.getEntityId()));
			NotificationPopup.showSuccess(msg, msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"), 
					msg.getMessage("ConnectId.ConfirmStep.mergeSuccessful"));
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("ConnectId.ConfirmStep.mergeFailed"), e);
		}
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
	public boolean onAdvance()
	{
		return authnContext != null;
	}

	@Override
	public boolean onBack()
	{
		return false;
	}
}
