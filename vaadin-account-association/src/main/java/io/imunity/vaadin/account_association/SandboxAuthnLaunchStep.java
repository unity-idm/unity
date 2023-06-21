/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.account_association;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import io.imunity.vaadin.account_association.wizard.WizardStep;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnNotifier;

import static java.util.Optional.ofNullable;

class SandboxAuthnLaunchStep extends WizardStep
{
	private final String callerId = VaadinService.getCurrentRequest().getWrappedSession().getId();
	private final UI parentUI = UI.getCurrent();
	private final InvocationContext originalInvocationContext = InvocationContext.getCurrent();
	private final SandboxAuthnNotifier sandboxAuthnNotifier;
	private final Runnable sandboxNewWindowOpener;
	private SandboxAuthnNotifier.AuthnResultListener listener;
	SandboxAuthnEvent event;
	Long sessionEntityId;

	public SandboxAuthnLaunchStep(String label, Component component, SandboxAuthnNotifier sandboxAuthnNotifier, Runnable sandboxNewWindowOpener)
	{
		super(label, component);
		this.sandboxAuthnNotifier = sandboxAuthnNotifier;
		this.sandboxNewWindowOpener = sandboxNewWindowOpener;
	}

	@Override
	protected void initialize()
	{
		this.event = null;
		this.sessionEntityId = null;
		sandboxNewWindowOpener.run();
		if(this.listener != null)
			sandboxAuthnNotifier.removeListener(this.listener);
		stepInProgress();
		this.listener = event ->
		{
				if (!callerId.equals(event.callerId))
					return;
				parentUI.access(() ->
				{
					{
						InvocationContext threadInvocationContext = InvocationContext.hasCurrent() ?
								InvocationContext.getCurrent() : null;

						InvocationContext.setCurrent(originalInvocationContext);
						try
						{
							this.event = event;
							this.sessionEntityId = ofNullable(InvocationContext.getCurrent().getLoginSession())
									.map(LoginSession::getEntityId)
									.orElse(null);
							stepRequiredNewStep();
							refreshWizard();
						} finally
						{
							InvocationContext.setCurrent(threadInvocationContext);
						}
				}});
		};
		sandboxAuthnNotifier.addListener(listener);
	}

	@Override
	protected void onDetach(DetachEvent detachEvent)
	{
		super.onDetach(detachEvent);
		sandboxAuthnNotifier.removeListener(this.listener);
	}
}
