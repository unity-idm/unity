/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.secured.shared.endpoint;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import io.imunity.vaadin23.secured.shared.endpoint.wizard.WizardStep;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnNotifier;

class SandboxStep extends WizardStep
{
	private final String callerId = VaadinService.getCurrentRequest().getWrappedSession().getId();
	private final UI parentUI = UI.getCurrent();
	private final InvocationContext originalInvocationContext = InvocationContext.getCurrent();
	private final SandboxAuthnNotifier sandboxAuthnNotifier;
	private final Runnable sandboxNewWindowOpener;
	private SandboxAuthnNotifier.AuthnResultListener listener;
	SandboxAuthnEvent event;

	public SandboxStep(String label, Component component, SandboxAuthnNotifier sandboxAuthnNotifier, Runnable sandboxNewWindowOpener)
	{
		super(label, component);
		this.sandboxAuthnNotifier = sandboxAuthnNotifier;
		this.sandboxNewWindowOpener = sandboxNewWindowOpener;
	}

	@Override
	protected void run()
	{
		this.event = null;
		sandboxNewWindowOpener.run();
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
							completed();
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
