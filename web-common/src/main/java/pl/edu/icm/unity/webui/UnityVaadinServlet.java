/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.security.cert.X509Certificate;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.CookieHelper;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.bus.EventsBus;

import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.SystemMessagesInfo;
import com.vaadin.server.SystemMessagesProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;


/**
 * Customization of the ordinary {@link VaadinServlet} using {@link VaadinUIProvider}
 * @author K. Benedyczak
 */
@SuppressWarnings("serial")
public class UnityVaadinServlet extends VaadinServlet
{
	public static final String LANGUAGE_COOKIE = "language";
	private transient ApplicationContext applicationContext;
	private transient UnityServerConfiguration config;
	private transient String uiBeanName;
	private transient EndpointDescription description;
	private transient List<Map<String, BindingAuthn>> authenticators;
	private transient CancelHandler cancelHandler;
	private transient SandboxAuthnRouter sandboxRouter;
	private transient EndpointRegistrationConfiguration registrationConfiguration;
	
	public UnityVaadinServlet(ApplicationContext applicationContext, String uiBeanName,
			EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration registrationConfiguration)
	{
		super();
		this.applicationContext = applicationContext;
		this.uiBeanName = uiBeanName;
		this.description = description;
		this.authenticators = authenticators;
		this.config = applicationContext.getBean(UnityServerConfiguration.class);
		this.registrationConfiguration = registrationConfiguration;
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException
	{
		Map<Class<?>, Object> saved = saveThreadLocalState();
		super.init(config);
		restoreThreadLocalState(saved);
		
		Object counter = getServletContext().getAttribute(UnsuccessfulAuthenticationCounter.class.getName());
		if (counter == null)
		{
			AuthenticationRealm realm = description.getRealm();
			getServletContext().setAttribute(UnsuccessfulAuthenticationCounter.class.getName(),
					new UnsuccessfulAuthenticationCounter(realm.getBlockAfterUnsuccessfulLogins(),
							realm.getBlockFor()*1000));
		}
		
		SystemMessagesProvider msgProvider = new SystemMessagesProvider() 
		{
			@Override 
			public SystemMessages getSystemMessages(
					SystemMessagesInfo systemMessagesInfo) {
				CustomizedSystemMessages messages =
						new CustomizedSystemMessages();
				messages.setCommunicationErrorCaption("It seems that your login session is no longer available");
				messages.setCommunicationErrorMessage("This happens most often due to "
						+ "prolonged inactivity. You have to log in again.");
				messages.setCommunicationErrorNotificationEnabled(true);
				messages.setCommunicationErrorURL(null);
				
				messages.setSessionExpiredCaption("Session expiration");
				messages.setSessionExpiredMessage("Your login session will expire in few seconds.");
				messages.setSessionExpiredURL(null);
				return messages;
			}
		}; 
		getService().setSystemMessagesProvider(msgProvider);
		
		getServletContext().getSessionCookieConfig().setHttpOnly(true);
	}
	
	private Map<Class<?>, Object> saveThreadLocalState()
	{
		Map<Class<?>, Object> saved = new HashMap<Class<?>, Object>();
		saved.put(UI.class, UI.getCurrent());
		saved.put(VaadinSession.class, VaadinSession.getCurrent());
		saved.put(VaadinServlet.class, VaadinServlet.getCurrent());
		saved.put(VaadinRequest.class, CurrentInstance.get(VaadinRequest.class));
		saved.put(VaadinResponse.class, CurrentInstance.get(VaadinResponse.class));
		return saved;
	}
	
	private void restoreThreadLocalState(Map<Class<?>, Object> saved)
	{
		UI ui = (UI) saved.get(UI.class);
		if (ui != null)
			UI.setCurrent(ui);
		
		VaadinSession session = (VaadinSession) saved.get(VaadinSession.class);
		if (session != null)
			VaadinSession.setCurrent(session);
		
		VaadinService service = (VaadinService) saved.get(VaadinService.class);
		if (service != null)
			VaadinService.setCurrent(service);

		VaadinRequest request = (VaadinRequest) saved.get(VaadinRequest.class);
		if (request != null)
			CurrentInstance.set(VaadinRequest.class, request);
		
		VaadinResponse response = (VaadinResponse) saved.get(VaadinResponse.class);
		if (response != null)
			CurrentInstance.set(VaadinResponse.class, response);
	}
	
	public synchronized void updateAuthenticators(List<Map<String, BindingAuthn>> authenticators)
	{
		this.authenticators = new ArrayList<>(authenticators);
	}
	
	protected synchronized List<Map<String, BindingAuthn>> getAuthenticators()
	{
		return this.authenticators;
	}
	
	public void setCancelHandler(CancelHandler cancelHandler)
	{
		this.cancelHandler = cancelHandler;
	}
	
	public void setSandboxRouter(SandboxAuthnRouter sandboxRouter) 
	{
		this.sandboxRouter = sandboxRouter;
	}
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException 
	{
		InvocationContext ctx = setEmptyInvocationContext(request);
		setAuthenticationContext(request, ctx);
		setLocale(request, ctx);
		try
		{
			super.service(request, response);
		} finally 
		{
			InvocationContext.setCurrent(null);
		}
	}
	
	private InvocationContext setEmptyInvocationContext(HttpServletRequest request)
	{
		X509Certificate[] clientCert = (X509Certificate[]) request.getAttribute(
				"javax.servlet.request.X509Certificate");
		IdentityTaV tlsId = (clientCert == null) ? null : new IdentityTaV(X500Identity.ID, 
				clientCert[0].getSubjectX500Principal().getName());
		InvocationContext context = new InvocationContext(tlsId, description.getRealm());
		InvocationContext.setCurrent(context);
		return context;
	}
	
	private void setAuthenticationContext(HttpServletRequest request, InvocationContext ctx)
	{
		HttpSession session = request.getSession(false);
		if (session != null)
		{
			LoginSession ls = (LoginSession) session.getAttribute(
					LoginToHttpSessionBinder.USER_SESSION_KEY);
			if (ls != null)
				ctx.setLoginSession(ls);
		}
	}
	
	/**
	 * Sets locale in invocation context. If there is cookie with selected and still supported
	 * locale then it is used. Otherwise a default locale is set.
	 * @param request
	 */
	private void setLocale(HttpServletRequest request, InvocationContext context)
	{
		String value = CookieHelper.getCookie(request, LANGUAGE_COOKIE);
		if (value != null)
		{
			Locale locale = UnityServerConfiguration.safeLocaleDecode(value);
			if (config.isLocaleSupported(locale))
			{
				context.setLocale(locale);
				return;
			}
		}
		context.setLocale(config.getDefaultLocale());
	}
	
	@Override
	protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) 
			throws ServiceException 
	{
		final VaadinServletService service = super.createServletService(deploymentConfiguration);

		service.addSessionInitListener(new SessionInitListener()
		{
			@Override
			public void sessionInit(SessionInitEvent event) throws ServiceException
			{
				VaadinUIProvider uiProv = new VaadinUIProvider(applicationContext, uiBeanName,
						description, getAuthenticators(), registrationConfiguration);
				uiProv.setCancelHandler(cancelHandler);
				uiProv.setSandboxRouter(sandboxRouter);
				event.getSession().addUIProvider(uiProv);
				DeploymentConfiguration depCfg = event.getService().getDeploymentConfiguration();
				Properties properties = depCfg.getInitParameters();
				String timeout = properties.getProperty(VaadinEndpoint.SESSION_TIMEOUT_PARAM);
				if (timeout != null)
					event.getSession().getSession().setMaxInactiveInterval(Integer.parseInt(timeout));

				if (WebSession.getCurrent() == null)
				{
					WebSession webSession = new WebSession(new EventsBus());
					WebSession.setCurrent(webSession);
				}			
			}
		});

		return service;
	}
}
