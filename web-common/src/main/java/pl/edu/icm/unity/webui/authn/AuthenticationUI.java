/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.ActivationListener;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.TopHeaderLight;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormsChooserComponent;
import pl.edu.icm.unity.webui.registration.RegistrationFormChooserDialog;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.Reindeer;



/**
 * Vaadin UI of the authentication application. Displays configured authenticators and 
 * coordinates authentication.
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component("AuthenticationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
@PreserveOnRefresh
public class AuthenticationUI extends UnityUIBase implements UnityWebUI
{
	private static final long serialVersionUID = 1L;
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationUI.class);
	private LocaleChoiceComponent localeChoice;
	private AuthenticationProcessor authnProcessor;
	private InsecureRegistrationFormsChooserComponent formsChooser;
	private InsecureRegistrationFormLauncher formLauncher;
	private ExecutorsService execService;
	private TopHeaderLight headerUIComponent;
	private AuthenticatorSetSelectComponent authnSelectionUIComponent;
	protected List<Map<String, VaadinAuthenticationUI>> authenticators;
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
			EndpointRegistrationConfiguration regCfg)
	{
		this.description = description;
		this.authenticators = new ArrayList<Map<String,VaadinAuthenticationUI>>();
		this.registrationConfiguration = regCfg;
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
	protected void appInit(final VaadinRequest request)
	{
		Component[] components = new Component[authenticators.size()];
		for (int i=0; i<components.length; i++)
			components[i] = new AuthenticatorSetComponent(authenticators.get(i), 
					description.getAuthenticatorSets().get(i), msg, authnProcessor, 
					formLauncher, execService, cancelHandler, description.getRealm());
		
		Button registrationButton = buildRegistrationButton();
		Component all = buildAllSetsUI(registrationButton, components);
		
		VerticalLayout main = new VerticalLayout();
		
		main.addComponent(localeChoice);
		main.setComponentAlignment(localeChoice, Alignment.TOP_LEFT);

		Label vSpacer = new Label("");
		main.addComponent(vSpacer);
		main.setExpandRatio(vSpacer, 1.0f);
		
		main.addComponent(all);
		main.setComponentAlignment(all, Alignment.TOP_CENTER);
		main.setExpandRatio(all, 5.0f);
		main.setSpacing(true);
		main.setMargin(true);
		main.setSizeFull();

		VerticalLayout topLevel = new VerticalLayout();
		headerUIComponent = new TopHeaderLight(msg.getMessage("AuthenticationUI.login", 
				description.getId()), msg);
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
		register.addStyleName(Reindeer.BUTTON_LINK);
		
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
		return register;
	}
	
	private Component buildAllSetsUI(final Button registrationButton, final Component... setComponents)
	{
		HorizontalLayout all = new HorizontalLayout();
		final Panel currentAuthnSet = new Panel();
		AuthenticatorSetChangedListener setChangeListener = new AuthenticatorSetChangedListener()
		{
			private ActivationListener last = null;
			
			@Override
			public void setWasChanged(int i)
			{
				Component c = setComponents[i];
				if (last != null)
					last.stateChanged(false);
				if (c instanceof ActivationListener)
					last = (ActivationListener)c;
				currentAuthnSet.setContent(getSingleSetUI(registrationButton, c));
			}
		};
		authnSelectionUIComponent = new AuthenticatorSetSelectComponent(msg, 
				setChangeListener, description, authenticators);

		currentAuthnSet.setContent(getSingleSetUI(registrationButton, setComponents[0]));
		
		all.addComponent(authnSelectionUIComponent);
		all.setComponentAlignment(authnSelectionUIComponent, Alignment.TOP_CENTER);
		all.addComponent(currentAuthnSet);
		all.setComponentAlignment(currentAuthnSet, Alignment.TOP_CENTER);
		all.setSpacing(true);
		all.setSizeFull();
		all.setExpandRatio(authnSelectionUIComponent, 1.0f);
		all.setExpandRatio(currentAuthnSet, 1.0f);
		if (setComponents.length == 1)
		{
			authnSelectionUIComponent.setVisible(false);
			currentAuthnSet.setWidth(50, Unit.PERCENTAGE);
			all.setComponentAlignment(currentAuthnSet, Alignment.TOP_RIGHT);
		}

		return all;
	}
	
	private Component getSingleSetUI(Button registrationButton, Component c)
	{
		if (c instanceof ActivationListener)
			((ActivationListener)c).stateChanged(true);
		
		if (registrationButton == null)
			return c;
		
		VerticalLayout vl = new VerticalLayout(c, registrationButton);
		vl.setSpacing(true);
		vl.setComponentAlignment(registrationButton, Alignment.BOTTOM_RIGHT);
		vl.setMargin(new MarginInfo(false, true, true, false));
		return vl;
	}
	
	@Override
	protected void refresh(VaadinRequest request) 
	{
		if (authenticators != null) {
			for (Map<String, VaadinAuthenticationUI> auth : authenticators)
			{
				for (VaadinAuthenticationUI authUI : auth.values())
				{
					authUI.refresh(request);
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
	
	protected void setSelectionTitle(String title)
	{
		if (authnSelectionUIComponent != null)
		{
			authnSelectionUIComponent.setSelectionTitle(title);
		}
	}	
}
