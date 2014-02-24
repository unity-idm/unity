/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.AttributesInternalProcessing;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationProcessorUtil;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.UI;

/**
 * Handles results of authentication and if it is all right, redirects to the source application.
 * 
 * TODO - this is far from being complete: needs to support fragments.
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticationProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationProcessor.class);
	
	private UnityMessageSource msg;
	private AuthenticationManagement authnMan;
	private IdentitiesManagement idsMan;
	private AttributesInternalProcessing attrProcessor;
	private CredentialEditorRegistry credEditorReg;
	
	@Autowired
	public AuthenticationProcessor(UnityMessageSource msg, AuthenticationManagement authnMan,
			IdentitiesManagement idsMan, AttributesInternalProcessing attrMan,
			CredentialEditorRegistry credEditorReg)
	{
		this.msg = msg;
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.attrProcessor = attrMan;
		this.credEditorReg = credEditorReg;
	}

	public void processResults(List<AuthenticationResult> results, String clientIp) throws AuthenticationException
	{
		UnsuccessfulAuthenticationCounter counter = getLoginCounter();
		AuthenticatedEntity logInfo;
		try
		{
			logInfo = AuthenticationProcessorUtil.processResults(results);
		} catch (AuthenticationException e)
		{
			if (!(e instanceof UnknownRemoteUserException))
				counter.unsuccessfulAttempt(clientIp);
			throw e;
		}
		setLabel(logInfo);
		WrappedSession session = logged(logInfo);

		if (logInfo.isUsedOutdatedCredential())
		{
			showCredentialUpdate();
			return;
		}
		redirectToOrigin(session);
	}

	private void setLabel(AuthenticatedEntity logInfo)
	{
		try
		{
			AttributeExt<?> attr = attrProcessor.getAttributeByMetadata(new EntityParam(logInfo.getEntityId()), "/", 
					EntityNameMetadataProvider.NAME);
			if (attr != null)
				logInfo.setEntityLabel((String) attr.getValues().get(0));
		} catch (AuthorizationException e)
		{
			log.debug("Not setting entity's label as the client is not authorized to read the attribute", e);
		} catch (EngineException e)
		{
			log.error("Can not get the attribute designated with EntityName", e);
		}
	}
	
	private void showCredentialUpdate()
	{
		OutdatedCredentialDialog dialog = new OutdatedCredentialDialog(msg, authnMan, idsMan, credEditorReg);
		dialog.show();
	}
	
	private static WrappedSession logged(AuthenticatedEntity authenticatedEntity) throws AuthenticationException
	{
		VaadinSession vss = VaadinSession.getCurrent();
		if (vss == null)
		{
			log.error("BUG: Can't get VaadinSession to store authenticated user's data.");
			throw new AuthenticationException("AuthenticationProcessor.authnInternalError");
		}
		WrappedSession session = vss.getSession();
		session.setAttribute(WebSession.USER_SESSION_KEY, authenticatedEntity);
		return session;
	}
	
	private static void redirectToOrigin(WrappedSession session) throws AuthenticationException
	{
		UI ui = UI.getCurrent();
		if (ui == null)
		{
			log.error("BUG Can't get UI to redirect the authenticated user.");
			throw new AuthenticationException("AuthenticationProcessor.authnInternalError");
		}
		String origURL = getOriginalURL(session);
		
		ui.getPage().open(origURL, "");
	}
	
	public static String getOriginalURL(WrappedSession session) throws AuthenticationException
	{
		String origURL = (String) session.getAttribute(AuthenticationFilter.ORIGINAL_ADDRESS);
		//String origFragment = (String) session.getAttribute(AuthenticationApp.ORIGINAL_FRAGMENT);
		if (origURL == null)
			throw new AuthenticationException("AuthenticationProcessor.noOriginatingAddress");
		//if (origFragment == null)
		//	origFragment = "";
		//else
		//	origFragment = "#" + origFragment;
		
		//origURL = origURL+origFragment;
		return origURL;
	}
	
	public static void logout()
	{
		VaadinSession vs = VaadinSession.getCurrent();
		final WrappedSession s = vs.getSession();
		Page p = Page.getCurrent();
		URI currentLocation = p.getLocation();
		//FIXME - workaround for the Vaadin bug http://dev.vaadin.com/ticket/12346
		// when the bug is fixed session should be invalidated in the current thread.
		// the workaround is ugly - it is possible that the 'logout' thread completes after we return answer
		// -> then user is logged out but no effect is visible automatically.
		Thread logoutThread = new Thread()
		{
			@Override
			public void run()
			{
				s.invalidate();
			}
		};
		logoutThread.setPriority(Thread.MAX_PRIORITY);
		logoutThread.start();
		p.setLocation(currentLocation);
	}
	
	/**
	 * Destroys the session and opens the original address again.
	 */
	public static void logoutAndRefresh()
	{
		VaadinSession vs = VaadinSession.getCurrent();
		WrappedSession s = vs.getSession();
		Page p = Page.getCurrent();
		String originalAddress;
		try
		{
			originalAddress = getOriginalURL(s);
		} catch (AuthenticationException e1)
		{
			originalAddress = p.getLocation().toString(); 
		}
		s.invalidate();
		p.setLocation(originalAddress);
	}
	
	public static UnsuccessfulAuthenticationCounter getLoginCounter()
	{
		HttpSession httpSession = ((WrappedHttpSession)VaadinSession.getCurrent().getSession()).getHttpSession();
		return (UnsuccessfulAuthenticationCounter) httpSession.getServletContext().getAttribute(
				UnsuccessfulAuthenticationCounter.class.getName());
	}
}
