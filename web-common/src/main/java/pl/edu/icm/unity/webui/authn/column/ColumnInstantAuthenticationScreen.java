/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_ADD_ALL;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMNS_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_SEPARATOR;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_TITLE;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_WIDTH;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_SHOW_CANCEL;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthNGridTextWrapper;
import pl.edu.icm.unity.webui.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.webui.authn.AuthenticationScreen;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.authn.tile.SelectedAuthNPanel;
import pl.edu.icm.unity.webui.authn.tile.SelectedAuthNPanel.AuthenticationListener;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Organizes authentication options in columns, making them instantly usable.
 * 
 * @author K. Benedyczak
 */
public class ColumnInstantAuthenticationScreen extends CustomComponent implements AuthenticationScreen
{
	private static final String SPECIAL_ENTRY_LAST_USED = "_LAST_USED";
	private static final String SPECIAL_ENTRY_REGISTER = "_REGISTER";
	private static final String SPECIAL_ENTRY_SEPARATOR = "_SEPARATOR";
	private static final String SPECIAL_ENTRY_HEADER = "_HEADER";
	protected final UnityMessageSource msg;
	private final VaadinEndpointProperties config;
	private final ResolvedEndpoint endpointDescription;
	private final Supplier<Boolean> outdatedCredentialDialogLauncher;
	private final Runnable registrationDialogLauncher;
	private final boolean enableRegistration;
	private final CancelHandler cancelHandler;
	
	private final EntityManagement idsMan;
	private final ExecutorsService execService;
	private final Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider;
	private final WebAuthenticationProcessor authnProcessor;	
	private final LocaleChoiceComponent localeChoice;
	
	private AuthenticationOptionsHandler authnOptionsHandler;
	private SelectedAuthNPanel authNPanelInProgress;
	private CheckBox rememberMe;
	private RemoteAuthenticationProgress authNProgress;
	
	public ColumnInstantAuthenticationScreen(UnityMessageSource msg, VaadinEndpointProperties config,
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
		this.authnOptionsHandler = new AuthenticationOptionsHandler(authenticators);
		init();
	}

	@Override
	public void refresh(VaadinRequest request) 
	{
		refreshAuthenticationState(request);
	}
	
	protected void init()
	{
		VerticalLayout topLevelLayout = new VerticalLayout();
		topLevelLayout.setMargin(new MarginInfo(false, true, false, true));
		topLevelLayout.setHeightUndefined();
		setCompositionRoot(topLevelLayout);

		Component languageChoice = getLanguageChoiceComponent();
		topLevelLayout.addComponent(languageChoice);
		topLevelLayout.setComponentAlignment(languageChoice, Alignment.TOP_CENTER);
		
		authNProgress = new RemoteAuthenticationProgress(msg, this::triggerAuthNCancel);
		topLevelLayout.addComponent(authNProgress);
		authNProgress.setInternalVisibility(false);
		topLevelLayout.setComponentAlignment(authNProgress, Alignment.TOP_RIGHT);
		
		Component authnOptionsComponent = getAuthenticationComponent();
		topLevelLayout.addComponent(authnOptionsComponent);
		topLevelLayout.setComponentAlignment(authnOptionsComponent, Alignment.MIDDLE_CENTER);
		
		if (outdatedCredentialDialogLauncher.get())
			return;
		
		//Extra safety - it can happen that we entered the UI in pipeline of authentication,
		// if this UI expired in the meantime. Shouldn't happen often as heart of authentication UI
		// is beating very slowly but in case of very slow user we may still need to refresh.
		refreshAuthenticationState(VaadinService.getCurrentRequest());
	}
	
	private Component getLanguageChoiceComponent()
	{
		HorizontalLayout languageChoiceLayout = new HorizontalLayout();
		languageChoiceLayout.setMargin(true);
		languageChoiceLayout.setSpacing(false);
		languageChoiceLayout.setWidth(100, Unit.PERCENTAGE);
		languageChoiceLayout.addComponent(localeChoice);
		languageChoiceLayout.setComponentAlignment(localeChoice, Alignment.MIDDLE_RIGHT);
		return languageChoiceLayout;
	}
	

	/**
	 * @return main authentication: logo, title, columns with authentication options
	 */
	private Component getAuthenticationComponent()
	{
		VerticalLayout authenticationMainLayout = new VerticalLayout();
		authenticationMainLayout.setMargin(false);
		
		String logoURL = config.getValue(VaadinEndpointProperties.AUTHN_LOGO);
		if (!logoURL.isEmpty())
		{
			Resource logoResource = ImageUtils.getConfiguredImageResource(logoURL);
			Image image = new Image(null, logoResource);
			image.addStyleName("u-authn-logo");
			authenticationMainLayout.addComponent(image);
			authenticationMainLayout.setComponentAlignment(image, Alignment.TOP_CENTER);
		}
		
		Component title = getTitleComponent();
		if (title != null)
		{
			authenticationMainLayout.addComponent(title);
			authenticationMainLayout.setComponentAlignment(title, Alignment.TOP_CENTER);
		}
		
		//TODO search support
		
		Component authNColumns = getAuthnColumnsComponent();
		authenticationMainLayout.addComponent(authNColumns);
		authenticationMainLayout.setComponentAlignment(authNColumns, Alignment.TOP_CENTER);
		
		AuthenticationRealm realm = endpointDescription.getRealm();
		if (realm.getAllowForRememberMeDays() > 0)
		{
			Component rememberMe = getRememberMeComponent(realm); 
			authenticationMainLayout.addComponent(rememberMe);
		}
		
		if (cancelHandler != null && config.getBooleanValue(AUTHN_SHOW_CANCEL))
		{
			authenticationMainLayout.addComponent(getCancelComponent());
		}
		
		return authenticationMainLayout;
	}
	
	private Component getCancelComponent()
	{
		Button cancel = new Button(msg.getMessage("AuthenticationUI.cancelAuthentication"));
		cancel.addStyleName(Styles.vButtonLink.toString());
		cancel.addClickListener(event -> {
			if (authNPanelInProgress != null)
				authNPanelInProgress.cancel();
			cancelHandler.onCancel();
		});
		HorizontalLayout bottomWrapper = new HorizontalLayout();
		bottomWrapper.setMargin(true);
		bottomWrapper.setWidth(100, Unit.PERCENTAGE);
		bottomWrapper.addComponent(cancel);
		bottomWrapper.setComponentAlignment(cancel, Alignment.TOP_CENTER);
		return bottomWrapper;
	}
	
	private Component getRememberMeComponent(AuthenticationRealm realm)
	{
		HorizontalLayout bottomWrapper = new HorizontalLayout();
		bottomWrapper.setMargin(true);
		bottomWrapper.setWidth(100, Unit.PERCENTAGE);
		rememberMe = new CheckBox(msg.getMessage("AuthenticationUI.rememberMe", 
				realm.getAllowForRememberMeDays()));
		rememberMe.addStyleName("u-authn-rememberMe");
		bottomWrapper.addComponent(rememberMe);
		bottomWrapper.setComponentAlignment(rememberMe, Alignment.TOP_RIGHT);
		return bottomWrapper;
	}
	
	private Component getTitleComponent()
	{
		String configuredMainTitle = config.getLocalizedValue(VaadinEndpointProperties.AUTHN_TITLE, msg.getLocale());
		String mainTitle = null;
		String serviceName = endpointDescription.getEndpoint().getConfiguration().getDisplayedName().getValue(msg);

		if (configuredMainTitle != null && !configuredMainTitle.isEmpty())
		{
			mainTitle = String.format(configuredMainTitle, serviceName);
		} else if (configuredMainTitle == null)
		{
			mainTitle = msg.getMessage("AuthenticationUI.login", serviceName);
		}
		if (mainTitle != null)
		{
			Label mainTitleLabel = new Label(mainTitle);
			mainTitleLabel.addStyleName("u-authn-title");
			return mainTitleLabel;
		}
		return null;
	}
	
	private Component getAuthnColumnsComponent()
	{
		HorizontalLayout columnsLayout = new HorizontalLayout();
		columnsLayout.setMargin(false);
		Iterator<String> columnKeys = config.getStructuredListKeys(AUTHN_COLUMNS_PFX).iterator();
		if (!columnKeys.hasNext())
		{
			//default layout
			AuthnOptionsColumn columnComponent = new AuthnOptionsColumn(null, DEFAULT_AUTHN_COLUMN_WIDTH);
			columnsLayout.addComponent(columnComponent);
			columnComponent.addOptions(getColumnAuthnComponents("", true));
			return columnsLayout;
		}
		boolean showAll = config.getBooleanValue(AUTHN_ADD_ALL);
		while (columnKeys.hasNext())
		{
			String columnKey = columnKeys.next();
			float width = (float)(double)config.getDoubleValue(columnKey+AUTHN_COLUMN_WIDTH);
			String title = config.getLocalizedValue(columnKey+AUTHN_COLUMN_TITLE, msg.getLocale());
			
			AuthnOptionsColumn columnComponent = new AuthnOptionsColumn(title, width);
			columnsLayout.addComponent(columnComponent);
			boolean addRemaining = !columnKeys.hasNext() && showAll;
			String spec = config.getValue(columnKey + AUTHN_COLUMN_CONTENTS);
			columnComponent.addOptions(getColumnAuthnComponents(spec, addRemaining));
			
			if (columnKeys.hasNext())
			{
				Component separator = getColumnsSeparator(columnKey);
				columnsLayout.addComponent(separator);
				columnsLayout.setComponentAlignment(separator, Alignment.MIDDLE_CENTER);
			}
		}
		
		return columnsLayout;
	}

	private Component getColumnsSeparator(String columnKey)
	{
		String separator = config.getLocalizedValue(columnKey+AUTHN_COLUMN_SEPARATOR, msg.getLocale());
		if (separator == null || separator.isEmpty())
			separator = "";
		Label separatorLabel = new Label(separator);
		separatorLabel.setStyleName("u-authn-columnsSeparator");
		return separatorLabel;
	}
	
	private boolean entryIsText(String entry)
	{
		return entry != null && (entry.startsWith(SPECIAL_ENTRY_SEPARATOR) 
				|| entry.startsWith(SPECIAL_ENTRY_HEADER));
	}
	
	private List<Component> getColumnAuthnComponents(String columnContents, boolean addRemaining)
	{
		String[] specSplit = columnContents.split("[ ]+");
		List<Component> ret = new ArrayList<>();
		Deque<String> lastAdded = new ArrayDeque<>();
		for (String specEntry: specSplit)
		{
			if (entryIsText(lastAdded.peek()) && entryIsText(specEntry))
			{
				ret.remove(ret.size()-1);
				lastAdded.pop();
			}
			
			if (specEntry.startsWith(SPECIAL_ENTRY_SEPARATOR))
			{
				if (ret.size() > 0)
				{
					ret.add(getOptionsSeparator(specEntry));
					lastAdded.push(specEntry);
				}
			} else if (specEntry.startsWith(SPECIAL_ENTRY_HEADER))
			{
				ret.add(getOptionHeader(specEntry));
				lastAdded.push(specEntry);
			} else if (specEntry.equals(SPECIAL_ENTRY_REGISTER))
			{
				Button registrationButton = buildRegistrationButton();
				if (registrationButton != null)
				{
					ret.add(registrationButton);
					lastAdded.push(specEntry);
				}
			} else if (specEntry.equals(SPECIAL_ENTRY_LAST_USED))
			{
				String preferredIdp = PreferredAuthenticationHelper.getPreferredIdp();
				if (preferredIdp != null)
				{
					AuthenticationOption authNOption = authnOptionsHandler.getMatchingOption(preferredIdp);
					VaadinAuthenticationUI vaadinAuthenticationUI = authnOptionsHandler.getFirstMatchingRetrieval(preferredIdp);
					if (vaadinAuthenticationUI != null)
					{
						SelectedAuthNPanel authNPanel = buildAuthenticationOptionWidget(authNOption, 
								vaadinAuthenticationUI);
						ret.add(authNPanel);
						lastAdded.push(specEntry);
					}
				}
			} else
			{
				AuthenticationOption authNOption = authnOptionsHandler.getMatchingOption(specEntry);
				List<VaadinAuthenticationUI> matchingRetrievals = authnOptionsHandler.getMatchingRetrievals(specEntry);
				for (VaadinAuthenticationUI vaadinAuthenticationUI : matchingRetrievals)
				{
					SelectedAuthNPanel authNPanel = buildAuthenticationOptionWidget(authNOption, 
							vaadinAuthenticationUI);
					ret.add(authNPanel);
					lastAdded.push(specEntry);
				}
			}
		}
		
		if (addRemaining)
		{
			Map<AuthenticationOption, List<VaadinAuthenticationUI>> remainingRetrievals = authnOptionsHandler.getRemainingRetrievals();
			for (Map.Entry<AuthenticationOption, List<VaadinAuthenticationUI>> option: remainingRetrievals.entrySet())
			{
				for (VaadinAuthenticationUI ui: option.getValue())
				{
					SelectedAuthNPanel authNPanel = buildAuthenticationOptionWidget(option.getKey(), ui);
					ret.add(authNPanel);
					lastAdded.push(AuthenticationOptionKeyUtils.encode(option.getKey().getId(), ui.getId()));
				}
			}
		}
		
		//Do not leave separator as a trailing entry
		while (entryIsText(lastAdded.peek()))
		{
			ret.remove(ret.size()-1);
			lastAdded.pop();
		}
		
		return ret;
	}
	
	private Component getOptionsSeparator(String specEntry)
	{
		String key = specEntry.substring(SPECIAL_ENTRY_SEPARATOR.length());
		
		String message = key.isEmpty() ? "" : resolveSeparatorMessage(key.substring(1));
		AuthNGridTextWrapper ret = new AuthNGridTextWrapper(new Label(message), Alignment.MIDDLE_CENTER);
		ret.setStyleName("u-authn-entriesSeparator");
		return ret;
	}

	private Component getOptionHeader(String specEntry)
	{
		String key = specEntry.substring(SPECIAL_ENTRY_HEADER.length());
		
		String message = key.isEmpty() ? "" : resolveSeparatorMessage(key.substring(1));
		AuthNGridTextWrapper ret = new AuthNGridTextWrapper(new Label(message), Alignment.MIDDLE_CENTER);
		ret.setStyleName("u-authn-entryHeader");
		return ret;
	}

	private String resolveSeparatorMessage(String key)
	{
		String value = config.getLocalizedValue(AUTHN_OPTION_LABEL_PFX + key + "." 
				+ AUTHN_OPTION_LABEL_TEXT, msg.getLocale());
		return value == null ? "" : value;
	}
	
	private SelectedAuthNPanel buildAuthenticationOptionWidget(AuthenticationOption authNOption, 
			VaadinAuthenticationUI vaadinAuthenticationUI)
	{
		SelectedAuthNPanel authNPanel = new SelectedAuthNPanel(msg, authnProcessor, 
				idsMan, execService, cancelHandler, 
				endpointDescription.getRealm(),
				endpointDescription.getEndpoint().getContextAddress(), 
				unknownUserDialogProvider,
				this::isSetRememberMe);
		authNPanel.setAuthenticationListener(new AuthenticationListenerImpl(authNPanel));
		String optionId = AuthenticationOptionKeyUtils.encode(authNOption.getId(), vaadinAuthenticationUI.getId()); 
		authNPanel.setAuthenticator(vaadinAuthenticationUI, authNOption, optionId);
		return authNPanel;
	}
	
	private boolean isSetRememberMe()
	{
		return rememberMe != null && rememberMe.getValue();
	}

	private Button buildRegistrationButton()
	{
		if (!enableRegistration)
			return null;
		Button register = new Button(msg.getMessage("RegistrationFormChooserDialog.register"));
		register.addStyleName("u-signUpButton");
		register.addClickListener(event -> registrationDialogLauncher.run());
		register.setId("AuthenticationUI.registerButton");
		return register;
	}
	
	private void refreshAuthenticationState(VaadinRequest request) 
	{
		if (authNPanelInProgress != null)
			authNPanelInProgress.refresh(request);
	}

	private void triggerAuthNCancel() 
	{
		if (authNPanelInProgress != null)
			authNPanelInProgress.cancel();
		authNProgress.setInternalVisibility(false);		
	}

	private class AuthenticationListenerImpl implements AuthenticationListener
	{
		private final SelectedAuthNPanel authNPanel;
		
		AuthenticationListenerImpl(SelectedAuthNPanel authNPanel)
		{
			this.authNPanel = authNPanel;
		}

		@Override
		public void authenticationStarted(boolean showProgress)
		{
			authNPanelInProgress = authNPanel;
			authNProgress.setInternalVisibility(showProgress);
			//TODO block other options
		}

		@Override
		public void authenticationStopped()
		{
			// TODO unblock options
			authNProgress.setInternalVisibility(false);
			authNPanelInProgress = null;
		}
	}
}
