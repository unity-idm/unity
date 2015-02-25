/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.Cookie;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.CookieHelper;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.authn.AuthNTile.SelectionChangedListener;
import pl.edu.icm.unity.webui.authn.SelectedAuthNPanel.AuthenticationListener;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.Styles;
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
	protected WebAuthenticationProcessor authnProcessor;
	protected InsecureRegistrationFormsChooserComponent formsChooser;
	protected InsecureRegistrationFormLauncher formLauncher;
	protected ExecutorsService execService;
	protected AuthenticationTopHeader headerUIComponent;
	protected SelectedAuthNPanel authenticationPanel;
	protected AuthNTiles selectorPanel;
	protected List<AuthenticationOption> authenticators;
	protected EndpointDescription description;
	protected EndpointRegistrationConfiguration registrationConfiguration;
	protected IdentitiesManagement idsMan;
	protected VaadinEndpointProperties config;
	
	@Autowired
	public AuthenticationUI(UnityMessageSource msg, LocaleChoiceComponent localeChoice,
			WebAuthenticationProcessor authnProcessor,
			InsecureRegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService, @Qualifier("insecure") IdentitiesManagement idsMan)
	{
		super(msg);
		this.localeChoice = localeChoice;
		this.authnProcessor = authnProcessor;
		this.formsChooser = formsChooser;
		this.formLauncher = formLauncher;
		this.execService = execService;
		this.idsMan = idsMan;
	}


	@Override
	public void configure(EndpointDescription description,
			List<AuthenticationOption> authenticators,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties genericEndpointConfiguration)
	{
		this.description = description;
		this.authenticators = new ArrayList<>(authenticators);
		this.registrationConfiguration = registrationConfiguration;
		config = new VaadinEndpointProperties(genericEndpointConfiguration);
	}

	@Override
	protected void appInit(final VaadinRequest request)
	{
		authenticationPanel = createSelectedAuthNPanel();
		authenticationPanel.addStyleName(Styles.minHeight300.toString());

		
		List<AuthNTile> tiles = prepareTiles(authenticators);
		selectorPanel = new AuthNTiles(msg, tiles);
		
		String lastIdp = getLastIdpFromCookie();
		String initialOption = null;
		if (lastIdp != null)
		{
			AuthenticationOption lastAuthnOption = selectorPanel.getAuthenticationOptionById(lastIdp);
			if (lastAuthnOption != null)
				initialOption = lastIdp;
		}
		if (initialOption == null)
			initialOption = tiles.get(0).getFirstOptionId();

		if (initialOption != null)
		{
			AuthenticationOption initAuthnOption = selectorPanel.getAuthenticationOptionById(initialOption);
			authenticationPanel.setAuthenticator(selectorPanel.getAuthenticatorById(initialOption), 
					initAuthnOption, initialOption);
			authenticationPanel.setVisible(true);
		}
		
		//language choice and registration
		HorizontalLayout topBar = new HorizontalLayout();
		topBar.setWidth(100, Unit.PERCENTAGE);
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
		main.setWidth(100, Unit.PERCENTAGE);
		main.setHeightUndefined();

		main.addComponent(authenticationPanel);
		main.setComponentAlignment(authenticationPanel, Alignment.TOP_CENTER);
		
		if (tiles.size() > 1 || tiles.get(0).size() > 1)
		{
			authenticationPanel.setAuthenticationListener(new AuthenticationListener()
			{
				@Override
				public void authenticationStateChanged(boolean started)
				{
					selectorPanel.setEnabled(!started);
				}
			});
			
			HorizontalLayout tilesWrapper = new HorizontalLayout();
			tilesWrapper.addComponent(selectorPanel);
			main.addComponent(tilesWrapper);
			main.setComponentAlignment(tilesWrapper, Alignment.TOP_CENTER);
			main.setExpandRatio(tilesWrapper, 1.0f);
		}
		
		VerticalLayout topLevel = new VerticalLayout();
		headerUIComponent = new AuthenticationTopHeader(msg.getMessage("AuthenticationUI.login", 
				description.getDisplayedName().getValue(msg)), localeChoice, msg);
		topLevel.addComponents(headerUIComponent, main);
		topLevel.setHeightUndefined();
		topLevel.setWidth(100, Unit.PERCENTAGE);
		topLevel.setExpandRatio(main, 1.0f);
		
		setContent(topLevel);
		setSizeFull();

		//Extra safety - it can happen that we entered the UI in pipeline of authentication,
		// if this UI expired in the meantime. Shouldn't happen often as heart of authentication UI
		// is beating very slowly but in case of very slow user we may still need to refresh.
		refresh(VaadinService.getCurrentRequest());
	}
	
	/**
	 * Overridden in sandboxed version
	 * @return
	 */
	protected SelectedAuthNPanel createSelectedAuthNPanel()
	{
		return new SelectedAuthNPanel(msg, authnProcessor, idsMan, formLauncher, 
				execService, cancelHandler, description.getRealm());
	}
	
	private List<AuthNTile> prepareTiles(List<AuthenticationOption> authenticators)
	{
		List<AuthNTile> ret = new ArrayList<>();
		List<AuthenticationOption> authNCopy = new ArrayList<>(authenticators);
		Set<String> tileKeys = config.getStructuredListKeys(VaadinEndpointProperties.AUTHN_TILES_PFX);
		ScaleMode defScaleMode = config.getEnumValue(VaadinEndpointProperties.DEFAULT_AUTHN_ICON_SIZE, 
				ScaleMode.class);
		int defPerRow = config.getIntValue(VaadinEndpointProperties.DEFAULT_PER_LINE);
		
		SelectionChangedListener selectionChangedListener = new SelectionChangedListener()
		{
			@Override
			public void selectionChanged(VaadinAuthenticationUI selectedAuthn,
					AuthenticationOption selectedOption, String globalId)
			{
				authenticationPanel.setAuthenticator(selectedAuthn, selectedOption, globalId);
				authenticationPanel.setVisible(true);
			}
		};
		
		for (String tileKey: tileKeys)
		{
			ScaleMode scaleMode = config.getEnumValue(tileKey + VaadinEndpointProperties.AUTHN_TILE_ICON_SIZE, 
					ScaleMode.class);
			if (scaleMode == null)
				scaleMode = defScaleMode;
			
			Integer perRow = config.getIntValue(tileKey + VaadinEndpointProperties.AUTHN_TILE_PER_LINE);
			if (perRow == null)
				perRow = defPerRow;
			
			String displayedName = config.getLocalizedValue(tileKey + 
					VaadinEndpointProperties.AUTHN_TILE_DISPLAY_NAME, msg.getLocale());
			
			String spec = config.getValue(tileKey + VaadinEndpointProperties.AUTHN_TILE_CONTENTS);
			String[] specSplit = spec.split("[ ]+");
			List<AuthenticationOption> authNs = new ArrayList<>();
			Iterator<AuthenticationOption> authNIt = authNCopy.iterator();
			while (authNIt.hasNext())
			{
				AuthenticationOption ao = authNIt.next();
				for (String matching: specSplit)
				{
					if (ao.getId().startsWith(matching))
					{
						authNs.add(ao);
						authNIt.remove();
					}
				}
			}
			AuthNTile tile = new AuthNTile(authNs, scaleMode, perRow, selectionChangedListener, 
					displayedName);
			ret.add(tile);
		}
		
		if (!authNCopy.isEmpty())
		{
			AuthNTile defaultTile = new AuthNTile(authNCopy, defScaleMode, defPerRow, 
					selectionChangedListener, null);
			ret.add(defaultTile);
		}
		
		return ret;
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
		authenticationPanel.refresh(request);
	}
	
	protected void setHeaderTitle(String title) 
	{
		if (headerUIComponent != null)
		{
			headerUIComponent.setHeaderTitle(title);
		}
	}

}
