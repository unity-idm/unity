/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.engine.authn.AuthenticatorLoader;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.authn.AuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormsChooserComponent;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;

/**
 * Vaadin UI of the sandbox application.
 *  
 * @author Roman Krysinski
 */
@org.springframework.stereotype.Component("SandboxUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
@PreserveOnRefresh
public class SandboxUI extends AuthenticationUI 
{
	private static final long serialVersionUID = 5093317898729462049L;
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, SandboxUI.class);
	
	private List<AuthenticatorSet> authnList;

	@Autowired
	public SandboxUI(UnityMessageSource msg,
			LocaleChoiceComponent localeChoice,
			AuthenticationProcessor authnProcessor,
			InsecureRegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService,
			AuthenticationManagement authnManagement,
			AuthenticatorLoader authnLoader,
			DBSessionManager db)
	{
		super(msg, localeChoice, authnProcessor, formsChooser, formLauncher, execService);
		
		authnList = new ArrayList<AuthenticatorSet>();
		try 
		{
			Collection<AuthenticatorInstance> authnInstances = authnManagement.getAuthenticators(VaadinAuthentication.NAME);
			if (LOG.isDebugEnabled()) 
			{
				StringBuilder builder = new StringBuilder();
				builder.append("authnInstances: \n");
				for (AuthenticatorInstance inst : authnInstances)
				{
					builder.append(inst.getId()).append("\n");
				}
				LOG.debug(builder.toString());
			}
			for (AuthenticatorInstance instance : authnInstances)
			{
				AuthenticatorSet authnSet = new AuthenticatorSet(Collections.singleton(instance.getId()));
				authnList.add(authnSet);
			}
		} catch (EngineException e) 
		{
			throw new IllegalStateException("Unable to initialize sandbox servlet: failed to get authenticators: " 
					+ e.getMessage(), e);
		}
		
		List<Map<String, BindingAuthn>> authenticators = null;
		SqlSession sql = db.getSqlSession(true);
		try 
		{
			authenticators = authnLoader.getAuthenticators(authnList, sql);
			sql.rollback();
		} catch (Exception e)
		{
			throw new IllegalStateException("Unable to initialize sandbox servlet: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
		
		this.authenticators = new ArrayList<Map<String, VaadinAuthenticationUI>>();
		for (int i=0; i<authenticators.size(); i++)
		{
			Map<String, VaadinAuthenticationUI> map = new HashMap<String, VaadinAuthenticationUI>();
			Map<String, BindingAuthn> origMap = authenticators.get(i);
			for (Map.Entry<String, BindingAuthn> el: origMap.entrySet())
				map.put(el.getKey(), ((VaadinAuthentication)el.getValue()).createUIInstance());
			this.authenticators.add(map);
		}		
		
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration regCfg) 
	{
		this.registrationConfiguration = new EndpointRegistrationConfiguration(false);
		this.description = description;
		this.description.setAuthenticatorSets(authnList);
	}
	
	@Override
	protected void appInit(VaadinRequest request) 
	{
		super.appInit(request);
	}
}
