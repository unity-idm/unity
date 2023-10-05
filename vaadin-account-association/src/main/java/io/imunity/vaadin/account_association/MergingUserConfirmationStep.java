/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.account_association;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.vaadin.account_association.wizard.WizardStep;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;

class MergingUserConfirmationStep extends WizardStep
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, MergingUserConfirmationStep.class);
	private final MessageSource msg;
	private final InputTranslationEngine translationEngine;
	SandboxAuthnContext ctx;
	Long sessionEntityId;



	public MergingUserConfirmationStep(String label, MessageSource msg, InputTranslationEngine translationEngine)
	{
		super(label, new HorizontalLayout());
		this.msg = msg;
		this.translationEngine = translationEngine;
	}

	@Override
	protected void initialize()
	{
		((HorizontalLayout)component).removeAll();
		if (ctx.getAuthnException().isPresent())
		{
			log.info("Sandbox got authn error", ctx.getAuthnException().get());
			setError(msg.getMessage("ConnectId.ConfirmStep.error"));
		} else if (ctx.getRemotePrincipal().isEmpty())
		{
			log.error("Tried to associate local account with another local account, what is unsupported and only remote account can be linked to a local one: {}", ctx);
			setError(msg.getMessage("Merging2LocalAccounts.error"));
		} else
		{
			if (!translationEngine.identitiesNotPresentInDb(ctx.getRemotePrincipal().get().getMappingResult()))
			{
				MappedIdentity existingIdentity = translationEngine.getExistingIdentity(
						ctx.getRemotePrincipal().get().getMappingResult());
				Entity existingEntity;
				try
				{
					existingEntity = translationEngine.resolveMappedIdentity(existingIdentity);
					if (existingEntity.getId().equals(sessionEntityId))
						setError(msg.getMessage("ConnectId.ConfirmStep.errorSameIdentity"));
					else
						setError(msg.getMessage("ConnectId.ConfirmStep.errorExistingIdentity"));
				} catch (EngineException e)
				{
					log.error("Shouldn't happen: existing identity can not be resolved", e);
					setError(msg.getMessage("ConnectId.ConfirmStep.errorExistingIdentity"));
				}
			} else
			{
				RemotelyAuthenticatedPrincipal authnContext = ctx.getRemotePrincipal().get();
				String message = msg.getMessage("ConnectId.ConfirmStep.info", authnContext.getRemoteIdPName());
				((HorizontalLayout)component).add(new Span(message));
				stepComplited();
				refreshWizard();
			}
		}
	}

	protected void setError(String message)
	{
		Icon icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
		icon.setClassName("warning-icon");
		((HorizontalLayout)component).add(icon);
		((HorizontalLayout)component).add(new Span(message));
		((HorizontalLayout)component).setAlignItems(FlexComponent.Alignment.CENTER);
		interrupt();
	}

	void prepareStep(SandboxAuthnEvent event, Long sessionEntityId)
	{
		this.ctx = event.ctx;
		this.sessionEntityId = sessionEntityId;
	}
}
