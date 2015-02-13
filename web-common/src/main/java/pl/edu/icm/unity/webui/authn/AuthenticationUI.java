/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.Cookie;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.CookieHelper;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.AuthNTile.SelectionChangedListener;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeaderLight;
import pl.edu.icm.unity.webui.common.idpselector.IdpSelectorComponent.ScaleMode;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormsChooserComponent;
import pl.edu.icm.unity.webui.registration.RegistrationFormChooserDialog;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;



/**
 * Vaadin UI of the authentication application. Displays configured authenticators and 
 * coordinates authentication.
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component("AuthenticationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
@PreserveOnRefresh
public class AuthenticationUI extends UnityUIBase implements UnityWebUI
{
	private static final long serialVersionUID = 1L;
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationUI.class);
	private static final String LAST_AUTHN_COOKIE = "lastAuthenticationUsed";
	
	protected LocaleChoiceComponent localeChoice;
	protected AuthenticationProcessor authnProcessor;
	protected InsecureRegistrationFormsChooserComponent formsChooser;
	protected InsecureRegistrationFormLauncher formLauncher;
	protected ExecutorsService execService;
	protected TopHeaderLight headerUIComponent;
	protected SelectedAuthNPanel authenticationPanel;
	protected AuthNTile selectorPanel;
	protected List<Map<String, VaadinAuthentication>> authenticators;
	protected EndpointDescription description;
	protected EndpointRegistrationConfiguration registrationConfiguration;
	
	@Autowired
	public AuthenticationUI(UnityMessageSource msg, LocaleChoiceComponent localeChoice,
			AuthenticationProcessor authnProcessor,
			InsecureRegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService)
	{
		super(msg);
		this.localeChoice = localeChoice;
		this.authnProcessor = authnProcessor;
		this.formsChooser = formsChooser;
		this.formLauncher = formLauncher;
		this.execService = execService;
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties)
	{
		this.description = description;
		this.authenticators = new ArrayList<Map<String,VaadinAuthentication>>();
		this.registrationConfiguration = regCfg;
		for (int i=0; i<authenticators.size(); i++)
		{
			Map<String, VaadinAuthentication> map = new HashMap<String, VaadinAuthentication>();
			Map<String, BindingAuthn> origMap = authenticators.get(i);
			for (Map.Entry<String, BindingAuthn> el: origMap.entrySet())
				map.put(el.getKey(), ((VaadinAuthentication)el.getValue()));
			this.authenticators.add(map);
		}
	}

	@Override
	protected void appInit(final VaadinRequest request)
	{
		authenticationPanel = new SelectedAuthNPanel(msg, authnProcessor, formLauncher, 
				execService, cancelHandler, description.getRealm());
		authenticationPanel.setVisible(false);
		selectorPanel = new AuthNTile(authenticators, ScaleMode.height50, 3, new SelectionChangedListener()
		{
			@Override
			public void selectionChanged(VaadinAuthenticationUI selectedOption, String globalId)
			{
				authenticationPanel.setAuthenticator(selectedOption, globalId);
				authenticationPanel.setVisible(true);
			}
		});
		
		String lastIdp = getLastIdpFromCookie();
		if (lastIdp != null)
		{
			VaadinAuthenticationUI lastUI = selectorPanel.getById(lastIdp);
			authenticationPanel.setAuthenticator(lastUI, lastIdp);
			authenticationPanel.setVisible(true);
		}
		
		//TODO add multiple tiles component with search
		
		//language choice and registration
		HorizontalLayout topBar = new HorizontalLayout();
		topBar.setWidth(100, Unit.PERCENTAGE);
		topBar.addComponent(localeChoice);
		topBar.setComponentAlignment(localeChoice, Alignment.TOP_LEFT);
		Button registrationButton = buildRegistrationButton();
		if (registrationButton != null)
		{
			topBar.addComponent(registrationButton);
			topBar.setComponentAlignment(registrationButton, Alignment.TOP_RIGHT);
		}
		
		VerticalLayout main = new VerticalLayout();
		main.addComponent(topBar);
		main.setSpacing(true);
		main.addStyleName(Styles.verticalMargins10.toString());
		main.setMargin(new MarginInfo(false, true, false, true));
		main.setSizeFull();

		main.addComponent(authenticationPanel);
		main.setComponentAlignment(authenticationPanel, Alignment.TOP_CENTER);

		main.addComponent(selectorPanel);
		main.setComponentAlignment(selectorPanel, Alignment.TOP_CENTER);
		main.setExpandRatio(selectorPanel, 1.0f);
		
		VerticalLayout topLevel = new VerticalLayout();
		headerUIComponent = new TopHeaderLight(msg.getMessage("AuthenticationUI.login", 
				description.getDisplayedName().getValue(msg)), msg);
		topLevel.addComponents(headerUIComponent, main);
		topLevel.setSizeFull();
		topLevel.setExpandRatio(main, 1.0f);
		
		setContent(topLevel);
		setSizeFull();
		
		//Extra safety - it can happen that we entered the UI in pipeline of authentication,
		// if this UI expired in the meantime. Shouldn't happen often as heart of authentication UI
		// is beating very slowly but in case of very slow user we may still need to refresh.
		refresh(VaadinService.getCurrentRequest()); 
	}
	
	public static void setLastIdpCookie(String idpKey)
	{
		VaadinResponse resp = VaadinService.getCurrentResponse();
		Cookie selectedIdp = new Cookie(LAST_AUTHN_COOKIE, idpKey);
		selectedIdp.setMaxAge(3600*24*30);
		selectedIdp.setPath("/");
		selectedIdp.setHttpOnly(true);
		resp.addCookie(selectedIdp);
	}
	
	private String getLastIdpFromCookie()
	{
		VaadinRequest req = VaadinService.getCurrentRequest();
		if (req == null)
			return null;
		return CookieHelper.getCookie(req.getCookies(), LAST_AUTHN_COOKIE);
	}
	
	private Button buildRegistrationButton()
	{
		if (!registrationConfiguration.isShowRegistrationOption())
			return null;
		if (registrationConfiguration.getEnabledForms().size() > 0)
			formsChooser.setAllowedForms(registrationConfiguration.getEnabledForms());
		formsChooser.initUI();
		if (formsChooser.getDisplayedForms().size() == 0)
			return null;
		
		Button register = new Button(msg.getMessage("RegistrationFormChooserDialog.register"));
		register.addStyleName(Styles.vButtonLink.toString());
		
		final AbstractDialog dialog;
		if (formsChooser.getDisplayedForms().size() == 1)
		{
			RegistrationForm form = formsChooser.getDisplayedForms().get(0);
			try
			{
				dialog = formLauncher.getDialog(form, new RemotelyAuthenticatedContext("--none--",
						"--none--"));
			} catch (EngineException e)
			{
				log.info("Can't initialize registration form '" + form.getName() + "' UI. "
						+ "It can be fine in some cases, but often means "
						+ "that the form should not be marked "
						+ "as public or its configuration is invalid: " + e.toString());
				if (log.isDebugEnabled())
					log.debug("Deatils: ", e);
				return null;
			}
		} else
		{
			dialog = new RegistrationFormChooserDialog(
				msg, msg.getMessage("RegistrationFormChooserDialog.selectForm"), formsChooser);
		}
		register.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				dialog.show();
			}
		});
		register.setId("AuthenticationUI.registerButton");
		return register;
	}
	
	@Override
	protected void refresh(VaadinRequest request) 
	{
		if (authenticators != null) 
		{
			for (Map<String, VaadinAuthentication> auth : authenticators)
			{
				for (VaadinAuthentication authUI : auth.values())
				{
					//FIXME
					//authUI.refresh(request);
				}
			}
		}
	}
	
	protected void setHeaderTitle(String title) 
	{
		if (headerUIComponent != null)
		{
			headerUIComponent.setHeaderTitle(title);
		}
	}
}
