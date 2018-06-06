/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.tile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.CookieHelper;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.VaadinEndpointProperties.TileMode;
import pl.edu.icm.unity.webui.authn.AuthenticationScreen;
import pl.edu.icm.unity.webui.authn.AuthenticationTopHeader;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.authn.tile.AuthNTile.SelectionChangedListener;
import pl.edu.icm.unity.webui.authn.tile.SelectedAuthNPanel.AuthenticationListener;
import pl.edu.icm.unity.webui.common.Styles;



/**
 * Tile based Authentication UI. User needs to select authN option from a tile and then authenticate. 
 * Tiles can come into two flavors: grid or plain options.
 * 
 * @author K. Benedyczak
 */
public class TileAuthenticationScreen extends CustomComponent implements AuthenticationScreen
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, TileAuthenticationScreen.class);
	private static final String LAST_AUTHN_COOKIE = "lastAuthnUsed";
	/**
	 * Query param allowing for selecting IdP in request to the endpoint
	 */
	public static final String IDP_SELECT_PARAM = "uy_select_authn";

	protected final UnityMessageSource msg;
	private final VaadinEndpointProperties config;
	private final ResolvedEndpoint endpointDescription;
	private final Supplier<Boolean> outdatedCredentialDialogLauncher;
	private final Runnable registrationDialogLauncher;
	private final CancelHandler cancelHandler;
	private final EntityManagement idsMan;
	private final ExecutorsService execService;
	private final boolean enableRegistration;
	private final Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider;
	private final WebAuthenticationProcessor authnProcessor;	
	private final LocaleChoiceComponent localeChoice;
	protected List<AuthenticationOption> authenticators;
	
	protected AuthenticationTopHeader headerUIComponent;
	private SelectedAuthNPanel authenticationPanel;
	protected AuthNTiles selectorPanel;
	private VerticalLayout topLevelLayout;

	public TileAuthenticationScreen(UnityMessageSource msg, VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			Supplier<Boolean> outdatedCredentialDialogLauncher,
			Runnable registrationDialogLauncher, CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, boolean enableRegistration,
			Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider,
			WebAuthenticationProcessor authnProcessor,
			LocaleChoiceComponent localeChoice,
			List<AuthenticationOption> authenticators)
	{
		this.msg = msg;
		this.config = config;
		this.endpointDescription = endpointDescription;
		this.outdatedCredentialDialogLauncher = outdatedCredentialDialogLauncher;
		this.registrationDialogLauncher = registrationDialogLauncher;
		this.cancelHandler = cancelHandler;
		this.idsMan = idsMan;
		this.execService = execService;
		this.enableRegistration = enableRegistration;
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.authnProcessor = authnProcessor;
		this.localeChoice = localeChoice;
		this.authenticators = authenticators;
		init();
	}

	@Override
	public void refresh(VaadinRequest request) 
	{
		refreshAuthenticationState(request);
	}
	
	protected void init()
	{
		authenticationPanel = createSelectedAuthNPanel();
		authenticationPanel.addStyleName(Styles.minHeightAuthn.toString());

		
		List<AuthNTile> tiles = prepareTiles(authenticators);
		selectorPanel = new AuthNTiles(msg, tiles, authenticationPanel);

		switchAuthnOptionIfRequested(tiles.get(0).getFirstOptionId());

		//language choice and registration
		HorizontalLayout topBar = new HorizontalLayout();
		topBar.setWidth(100, Unit.PERCENTAGE);
		topBar.setSpacing(false);
		topBar.setMargin(false);
		Button registrationButton = buildRegistrationButton();
		if (registrationButton != null)
		{
			topBar.addComponent(registrationButton);
			topBar.setComponentAlignment(registrationButton, Alignment.TOP_RIGHT);
		}
		
		VerticalLayout main = new VerticalLayout();
		main.addComponent(topBar);
		main.setMargin(new MarginInfo(false, true, false, true));
		main.setWidth(100, Unit.PERCENTAGE);
		main.setHeightUndefined();

		main.addComponent(authenticationPanel);
		main.setComponentAlignment(authenticationPanel, Alignment.TOP_CENTER);
		
		authenticationPanel.setAuthenticationListener(new AuthenticationListener()
		{
			@Override
			public void authenticationStateChanged(boolean started)
			{
				if (tiles.size() > 1 || tiles.get(0).size() > 1)
					selectorPanel.setEnabled(!started);
			}

			@Override
			public void clearUI()
			{
				setCompositionRoot(new VerticalLayout());
			}
		});
		
		if (tiles.size() > 1 || tiles.get(0).size() > 1)
		{
			HorizontalLayout tilesWrapper = new HorizontalLayout();
			tilesWrapper.addComponent(selectorPanel);
			tilesWrapper.setSpacing(false);
			tilesWrapper.setMargin(false);
			main.addComponent(tilesWrapper);
			main.setComponentAlignment(tilesWrapper, Alignment.TOP_CENTER);
			main.setExpandRatio(tilesWrapper, 1.0f);
		}
		
		topLevelLayout = new VerticalLayout();
		headerUIComponent = new AuthenticationTopHeader(msg.getMessage("AuthenticationUI.login", 
				endpointDescription.getEndpoint().getConfiguration().getDisplayedName().getValue(msg)), 
				localeChoice, msg);
		topLevelLayout.addComponents(headerUIComponent, main);
		topLevelLayout.setHeightUndefined();
		topLevelLayout.setWidth(100, Unit.PERCENTAGE);
		topLevelLayout.setExpandRatio(main, 1.0f);
		topLevelLayout.setSpacing(false);
		topLevelLayout.setMargin(false);
		
		setCompositionRoot(topLevelLayout);
		setSizeFull();
		
		if (outdatedCredentialDialogLauncher.get())
			return;
		
		//Extra safety - it can happen that we entered the UI in pipeline of authentication,
		// if this UI expired in the meantime. Shouldn't happen often as heart of authentication UI
		// is beating very slowly but in case of very slow user we may still need to refresh.
		refreshAuthenticationState(VaadinService.getCurrentRequest());
	}
	
	private SelectedAuthNPanel createSelectedAuthNPanel()
	{
		return new SelectedAuthNPanel(msg, authnProcessor, idsMan,  
				execService, cancelHandler, endpointDescription.getRealm(),
				endpointDescription.getEndpoint().getContextAddress(),
				unknownUserDialogProvider);
	}
	
	private List<AuthNTile> prepareTiles(List<AuthenticationOption> authenticators)
	{
		List<AuthNTile> ret = new ArrayList<>();
		List<AuthenticationOption> authNCopy = new ArrayList<>(authenticators);
		Set<String> tileKeys = config.getStructuredListKeys(VaadinEndpointProperties.AUTHN_TILES_PFX);
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
			
			ScaleMode scaleMode = config.getScaleMode(tileKey);
			
			Integer perRow = config.getIntValue(tileKey + VaadinEndpointProperties.AUTHN_TILE_PER_LINE);
			if (perRow == null)
				perRow = defPerRow;
			
			TileMode tileMode = config.getEnumValue(tileKey + VaadinEndpointProperties.AUTHN_TILE_TYPE,
					TileMode.class);
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
			AuthNTile tile = tileMode == TileMode.simple ? 
				new AuthNTileSimple(authNs, scaleMode, perRow, selectionChangedListener, displayedName) : 
				new AuthNTileGrid(authNs, msg, selectionChangedListener, displayedName);
			log.debug("Adding tile with authenticators {}", tile.getAuthenticators().keySet());
			ret.add(tile);
		}
		
		if (!authNCopy.isEmpty())
		{
			AuthNTile defaultTile = new AuthNTileSimple(authNCopy, config.getDefaultScaleMode(), 
					defPerRow, selectionChangedListener, null);
			ret.add(defaultTile);
		}
		
		return ret;
	}

	private String getLastIdpFromRequestParam()
	{
		VaadinRequest req = VaadinService.getCurrentRequest();
		if (req == null)
			return null;
		return req.getParameter(IDP_SELECT_PARAM);
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
		if (!enableRegistration)
			return null;
		Button register = new Button(msg.getMessage("RegistrationFormChooserDialog.register"));
		register.addStyleName(Styles.vButtonLink.toString());
		register.addClickListener(event -> registrationDialogLauncher.run());
		register.setId("AuthenticationUI.registerButton");
		return register;
	}
	
	private void switchAuthnOptionIfRequested(String defaultVal)
	{
		String lastIdp = getLastIdpFromRequestParam(); 
		if (lastIdp == null)
			lastIdp = getLastIdpFromCookie();
		String initialOption = null;
		if (lastIdp != null)
		{
			AuthenticationOption lastAuthnOption = selectorPanel.getAuthenticationOptionById(lastIdp);
			if (lastAuthnOption != null)
				initialOption = lastIdp;
		}
		log.debug("Requested/cookie idp={}, is available={}", lastIdp, initialOption!=null);
		if (initialOption == null)
			initialOption = defaultVal;

		if (initialOption != null)
		{
			AuthenticationOption initAuthnOption = selectorPanel.getAuthenticationOptionById(initialOption);
			authenticationPanel.setAuthenticator(selectorPanel.getAuthenticatorById(initialOption), 
					initAuthnOption, initialOption);
			authenticationPanel.setVisible(true);
		}
	}
	
	private void refreshAuthenticationState(VaadinRequest request) 
	{
		setCompositionRoot(topLevelLayout);	//in case somebody refreshes UI which was previously replaced with empty
							//may happen that the following code will clean it but it is OK.
		switchAuthnOptionIfRequested(null);
		authenticationPanel.refresh(request);
	}
}
