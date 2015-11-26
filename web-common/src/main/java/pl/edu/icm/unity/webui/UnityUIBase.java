/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Stack;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.annotations.Push;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.UI;

/**
 * All Unity {@link UI}s should extend this class. It provides a common logic. 
 * Currently proper error handling of unchecked exceptions.
 * @author K. Benedyczak
 */
@Push(value=PushMode.DISABLED)
public abstract class UnityUIBase extends UI implements UnityWebUI
{
	/**
	 * Under this key a {@link Queue} of Runnables is stored. The runnables are run by the 
	 * {@link RequestsContextQueueHandler}.
	 */
	public static final String ACTIONS_LIST_KEY = "pl.edu.icm.unity.web.WebSession.actionsList";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UnityUIBase.class);
	
	protected UnityMessageSource msg;
	protected CancelHandler cancelHandler;
	protected SandboxAuthnRouter sandboxRouter;
	protected VaadinEndpointProperties config;
	protected EndpointDescription endpointDescription;
	
	private Stack<Integer> pollings = new Stack<>();
	
	public UnityUIBase(UnityMessageSource msg)
	{
		super();
		this.msg = msg;
	}

	/**
	 * Default implementation saves the endpoint's description and initializes {@link VaadinEndpointProperties}
	 * in {@link #config}. Typically it is a good choice to call this super method when overriding.
	 */
	@Override
	public void configure(EndpointDescription description,
			List<AuthenticationOption> authenticators,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties genericEndpointConfiguration)
	{
		this.endpointDescription = description;
		config = new VaadinEndpointProperties(genericEndpointConfiguration);
	}

	
	@Override
	protected final void init(VaadinRequest request)
	{
		setErrorHandler(new ErrorHandlerImpl());
		appInit(request);
	}

	@Override
	public void setCancelHandler(CancelHandler handler)
	{
		this.cancelHandler = handler;
	}
	
	@Override
	public void setSandboxRouter(SandboxAuthnRouter sandboxRouter) 
	{
		this.sandboxRouter = sandboxRouter;
	}
	
	/**
	 * @return sandbox servlet URL which can be used for account association login. 
	 * Note that this URL is always fine but the servlet might not be deployed
	 * under it, if your wrapping endpoint does not extend {@link VaadinEndpointWithSandbox}.  
	 */
	public String getSandboxServletURLForAssociation()
	{
		return endpointDescription.getContextAddress() + VaadinEndpoint.SANDBOX_PATH_ASSOCIATION;
	}

	/**
	 * @return sandbox servlet URL which can be used for translation profile wizards. 
	 * Note that this URL is always fine but the servlet might not be deployed
	 * under it, if your wrapping endpoint does not extend {@link VaadinEndpointWithSandbox}.  
	 */
	public String getSandboxServletURLForTranslation()
	{
		return endpointDescription.getContextAddress() + VaadinEndpoint.SANDBOX_PATH_TRANSLATION;
	}
	
	/**
	 * This method is overriden to ensure that multiple components can individually manipulate poll intervals.
	 * If multiple pollings are turned on then the interval is set to a smallest value. What's more the polling
	 * is stopped only when all registered pollings are removed. The stack used is not really needed - 
	 * we could use counter.
	 * @param interval
	 */
	@Override
	public void setPollInterval(int interval)
	{
		log.debug("Set Poll wrapped " + interval);
		if (interval < 0)
		{
			if (!pollings.isEmpty())
				pollings.pop();
			if (pollings.isEmpty())
			{
				log.debug("Poll disabled");
				super.setPollInterval(-1);
			}
		} else
		{
			pollings.push(interval);
			int currentPoll = super.getPollInterval();
			if (currentPoll < 0 || currentPoll > interval)
			{
				log.debug("Poll enabled");
				super.setPollInterval(interval);
			}
		}
	}
	
	/**
	 * Same as Vaadin's {@link #init(VaadinRequest)}, separated so 
	 * it is not possible to forgot to call super.init().
	 * @param request
	 */
	protected abstract void appInit(VaadinRequest request);

	private class ErrorHandlerImpl extends DefaultErrorHandler 
	{
		@Override
		public void error(com.vaadin.server.ErrorEvent event) {
			
			log.error("UI code got an unchecked and not handled properly exception: " 
					+ event.getThrowable(), event.getThrowable());
			
			NotificationPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("UnityUIBase.unhandledError"));
		} 
	}
}
