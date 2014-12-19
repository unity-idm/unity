package pl.edu.icm.unity.verifiers;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.ServerInitializer;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.VaadinUIProvider;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;

@Component
public class EmailVerificatorInitializer implements ServerInitializer
{
	private static Logger log = Log.getLogger(Log.U_SERVER, EmailVerificatorInitializer.class);
	public static final String SERVLET_PATH = "/confirmation";	
	public static final String NAME = "emailVerificatorInitializer";	
	private ApplicationContext applicationContext;

	private SharedEndpointManagement sharedEndpointManagement;

	@Autowired
	public EmailVerificatorInitializer(ApplicationContext applicationContext,
			SharedEndpointManagement sharedEndpointManagement)
	{
		this.applicationContext = applicationContext;
		this.sharedEndpointManagement = sharedEndpointManagement;
	}

	@Override
	public void run()
	{
		VaadinServlet emailServlet = new ConfirmationServlet();	
		ServletHolder holder = new ServletHolder(emailServlet);
		try
		{
			sharedEndpointManagement.deployInternalEndpointServlet(SERVLET_PATH, holder, true);	
		} catch (EngineException e)
		{
			log.error("Cannot deploy internal email verificator servlet", e);
		}
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	public class ConfirmationServlet extends VaadinServlet
	{
				
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
					VaadinUIProvider uiProv = new VaadinUIProvider(applicationContext, EmailVerificationUI.class.getSimpleName(),
							null, null, null);
					//uiProv.setCancelHandler(cancelHandler);
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
}
