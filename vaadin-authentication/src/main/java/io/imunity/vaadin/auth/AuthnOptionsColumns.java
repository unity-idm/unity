/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.LinkButton;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;

import java.util.*;

import static io.imunity.vaadin.auth.AuthnOptionsColumn.ComponentWithId;
import static io.imunity.vaadin.auth.AuthnOptionsColumn.ComponentWithId.createNonLoginComponent;
import static io.imunity.vaadin.auth.AuthnOptionsColumn.ComponentWithId.createSimpleLoginComponent;
import static io.imunity.vaadin.elements.CSSVars.BIG_MARGIN;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.*;

/**
 * Core component maintaining set of columns with authentication options.
 * Allow for general operations like disabling or hiding.
 */
public class AuthnOptionsColumns extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthnOptionsColumns.class);
	public static final String SPECIAL_ENTRY_LAST_USED = "_LAST_USED";
	public static final String SPECIAL_ENTRY_REGISTER = "_REGISTER";
	public static final String SPECIAL_ENTRY_SEPARATOR = "_SEPARATOR";
	public static final String SPECIAL_ENTRY_HEADER = "_HEADER";
	public static final String SPECIAL_ENTRY_GRID = "_GRID_";
	public static final String SPECIAL_ENTRY_EXPAND = "_EXPAND"; //note that this one is not documented, for internal use
	
	private final VaadinEndpointProperties config;
	private final MessageSource msg;
	private final AuthenticationOptionsHandler authnOptionsHandler;
	private final boolean enableRegistration;
	private final AuthNPanelFactory authNPanelFactory;
	private final Runnable registrationLayoutLauncher;
	
	private final List<AuthnOptionsColumn> columns;
	
	AuthnOptionsColumns(VaadinEndpointProperties config, MessageSource msg,
	                    AuthenticationOptionsHandler authnOptionsHandler, boolean enableRegistration,
	                    AuthNPanelFactory authNPanelFactory,
	                    Runnable registrationLayoutLauncher)
	{
		this.config = config;
		this.msg = msg;
		this.authnOptionsHandler = authnOptionsHandler;
		this.enableRegistration = enableRegistration;
		this.authNPanelFactory = authNPanelFactory;
		this.registrationLayoutLauncher = registrationLayoutLauncher;
		
		this.columns = new ArrayList<>();
		setRootComponent(getAuthnColumnsComponent());
		setSizeUndefined();
		setMargin(false);
		setPadding(false);
		focusFirst();
	}

	void disableAllExcept(String exception)
	{
		for (AuthnOptionsColumn column: columns)
			column.disableAllExcept(exception);
	}
	
	void enableAll()
	{
		for (AuthnOptionsColumn column: columns)
			column.enableAll();
	}
	

	void focusFirst()
	{
		for (AuthnOptionsColumn column: columns)
		{
			if (column.focusFirst())
				break;
		}
	}
	
	void filter(String filter)
	{
		for (AuthnOptionsColumn column: columns)
			column.filter(filter);
	}
	
	private void setRootComponent(Component component)
	{
		removeAll();
		if (config.getBooleanValue(AUTHN_SHOW_SEARCH) && hasGridWidget())
		{
			VerticalLayout vWrapper = new VerticalLayout();
			vWrapper.setSizeUndefined();
			vWrapper.setMargin(false);
			vWrapper.setPadding(false);
			SearchComponent search = new SearchComponent(msg, this::filter);
			vWrapper.add(search);
			vWrapper.setAlignItems(Alignment.END);
			
			vWrapper.add(component);
			add(vWrapper);
		} else
		{
			add(component);
		}
	}


	private Component getAuthnColumnsComponent()
	{
		Component fullAuthnColumnsComponent = getFullAuthnColumnsComponent();
		if (log.isDebugEnabled())
			log.debug("Returning user UI decision: (config: {} preferredIdp: {} multipleOptionsConfigured: {})",
					config.getBooleanValue(AUTHN_SHOW_LAST_OPTION_ONLY),
					PreferredAuthenticationHelper.getPreferredIdp(),
					hasMoreThenOneOptionConfigured());
		if (config.getBooleanValue(AUTHN_SHOW_LAST_OPTION_ONLY) && 
				PreferredAuthenticationHelper.getPreferredIdp() != null &&
				hasMoreThenOneOptionConfigured())
		{
			authnOptionsHandler.clear();
			Component lastSelectionComponent = createLastSelectionLayout();
			if (lastSelectionComponent != null)
			{
				return lastSelectionComponent;
			} else
			{
				log.debug("UI for the returning user was not created, falling back to default screen");
			}
		}
		return fullAuthnColumnsComponent;
	}

	private Component getFullAuthnColumnsComponent()
	{
		authnOptionsHandler.clear();
		Set<String> columnsKeys = config.getStructuredListKeys(AUTHN_COLUMNS_PFX);
		log.trace("Columns prefixes: {}", columnsKeys);
		Iterator<String> columnKeys = columnsKeys.iterator();
		if (!columnKeys.hasNext())
		{
			log.trace("Creating default layout");
			return createDefaultLayout();
		} else
		{
			log.trace("Creating standard expanded layout");
			return createStandardExpandedLayout(columnKeys);
		}
	}

	
	private Component createDefaultLayout()
	{
		HorizontalLayout columnsLayout = new HorizontalLayout();
		columnsLayout.setMargin(false);
		AuthnOptionsColumn columnComponent = new AuthnOptionsColumn(null, DEFAULT_AUTHN_COLUMN_WIDTH);
		columns.clear();
		columns.add(columnComponent);
		columnsLayout.add(columnComponent);
		columnComponent.addOptions(getColumnAuthnComponents("", true));
		return columnsLayout;
	}

	private Component createLastSelectionLayout()
	{
		HorizontalLayout columnsLayout = new HorizontalLayout();
		columnsLayout.setMargin(false);
		String layout = config.getValue(AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT);
		List<ComponentWithId> authnComponents = getColumnAuthnComponents(layout, false);
		if (authnComponents.isEmpty())
			return null;
		AuthnOptionsColumn columnComponent = new AuthnOptionsColumn(null, DEFAULT_AUTHN_COLUMN_WIDTH);
		columns.clear();
		columns.add(columnComponent);
		columnsLayout.add(columnComponent);
		columnComponent.addOptions(authnComponents);
		return columnsLayout;
	}
	
	private boolean hasMoreThenOneOptionConfigured()
	{
		int count = 0;
		for (AuthnOptionsColumn column: columns)
		{
			count += column.countAuthenticationOptions();
			if (count > 1)
				return true;
		}
		return false;
	}
	
	
	private boolean hasGridWidget()
	{
		for (AuthnOptionsColumn column: columns)
		{
			if (column.hasGridWidget())
				return true;
		}
		return false;
	}
	
	private Component createStandardExpandedLayout(Iterator<String> columnKeys)
	{
		HorizontalLayout columnsLayout = new HorizontalLayout();
		columnsLayout.setMargin(false);
		columnsLayout.setPadding(false);
		columnsLayout.setWidthFull();
		boolean showAll = config.getBooleanValue(AUTHN_ADD_ALL);
		boolean focussed = false;
		columns.clear();
		while (columnKeys.hasNext())
		{
			String columnKey = columnKeys.next();
			float width = (float)(double)config.getDoubleValue(columnKey+AUTHN_COLUMN_WIDTH);
			String title = config.getLocalizedValue(columnKey+AUTHN_COLUMN_TITLE, msg.getLocale());
			
			boolean addRemaining = !columnKeys.hasNext() && showAll;
			String spec = config.getValue(columnKey + AUTHN_COLUMN_CONTENTS);
			List<ComponentWithId> columnAuthnComponents = getColumnAuthnComponents(spec, addRemaining);
			
			if (columnAuthnComponents.isEmpty())
				continue;
			
			AuthnOptionsColumn columnComponent = new AuthnOptionsColumn(title, width);
			columnComponent.addClassName("u-auto-width");
			columnsLayout.add(columnComponent);
			columns.add(columnComponent);
			columnComponent.addOptions(columnAuthnComponents);
			
			if (!focussed)
				focussed = columnComponent.focusFirst();
			
			if (columnKeys.hasNext())
			{
				Component separator = getColumnsSeparator(columnKey);
				columnsLayout.add(separator);
			}
		}
		log.trace("Created {} columns", columns.size());
		return columnsLayout;
	}

	
	private List<ComponentWithId> getColumnAuthnComponents(String columnContents, boolean addRemaining)
	{
		log.trace("Generating column for spec: {} (add remaining: {})", columnContents, addRemaining);
		String[] specSplit = columnContents.trim().split("[ ]+");
		List<ComponentWithId> ret = new ArrayList<>();
		Deque<String> lastAdded = new ArrayDeque<>();
		for (String specEntry: specSplit)
		{
			if (specEntry.isEmpty())
				continue;
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
					ret.add(createNonLoginComponent(specEntry, registrationButton));
					lastAdded.push(specEntry);
				}
			} else if (specEntry.equals(SPECIAL_ENTRY_LAST_USED))
			{
				String preferredIdp = PreferredAuthenticationHelper.getPreferredIdp();
				if (preferredIdp != null)
				{
					AuthNOption authnOption = authnOptionsHandler.getFirstMatchingOption(preferredIdp);
					if (authnOption != null)
					{
						FirstFactorAuthNPanel authNPanel = authNPanelFactory.createRegularAuthnPanel(authnOption);
						ret.add(createSimpleLoginComponent(
								authNPanel.getAuthenticationOptionId().toStringEncodedKey(), authNPanel));
						lastAdded.push(specEntry);
					}
				}
			}  else if (specEntry.equals(SPECIAL_ENTRY_EXPAND))
			{
				if (lastAdded.contains(SPECIAL_ENTRY_LAST_USED))
				{
					ret.add(getExpandAllOptionsButton());
					lastAdded.push(specEntry);
				}
			} else if (specEntry.startsWith(SPECIAL_ENTRY_GRID))
			{
				ComponentWithId grid = getGrid(specEntry);
				if (grid != null)
				{
					ret.add(grid);
					lastAdded.push(specEntry);
				}
			} else
			{
				List<AuthNOption> matchingOptions = authnOptionsHandler.getMatchingAuthnOptions(specEntry);
				for (AuthNOption authnOption : matchingOptions)
				{
					FirstFactorAuthNPanel authNPanel = authNPanelFactory.createRegularAuthnPanel(authnOption);
					ret.add(createSimpleLoginComponent(authNPanel.getAuthenticationOptionId().toStringEncodedKey(), authNPanel));
					lastAdded.push(specEntry);
				}
			}
		}
		if (addRemaining)
		{
			List<AuthNOption> remainingRetrievals = authnOptionsHandler.getRemainingAuthnOptions();
			for (AuthNOption entry: remainingRetrievals)
			{
				FirstFactorAuthNPanel authNPanel = authNPanelFactory.createRegularAuthnPanel(entry);
				ret.add(createSimpleLoginComponent(authNPanel.getAuthenticationOptionId().toStringEncodedKey(), authNPanel));
				lastAdded.push(AuthenticationOptionKeyUtils.encode(entry.authenticator.getAuthenticatorId(), 
						entry.authenticatorUI.getId()));
			}
		}
		
		//Do not leave separator as a trailing entry
		while (entryIsText(lastAdded.peek()))
		{
			ret.remove(ret.size()-1);
			lastAdded.pop();
		}
		log.trace("Generated column with {} elements", ret.size());
		return ret;
	}
	
	private ComponentWithId getExpandAllOptionsButton()
	{
		LinkButton expand = new LinkButton(msg.getMessage("AuthenticationUI.showAllOptions"), event -> showAllOptions());
		expand.getStyle().set("margin-top", BIG_MARGIN.value());
		return createNonLoginComponent(SPECIAL_ENTRY_EXPAND, expand);
	}

	private void showAllOptions()
	{
		setRootComponent(getFullAuthnColumnsComponent());
		focusFirst();
	}

	private ComponentWithId getOptionsSeparator(String specEntry)
	{
		String key = specEntry.substring(SPECIAL_ENTRY_SEPARATOR.length());
		
		String message = key.isEmpty() ? "" : resolveSeparatorMessage(key.substring(1));
		AuthNGridTextWrapper ret = new AuthNGridTextWrapper(new Span(message), Alignment.CENTER);
		ret.setClassName("u-authn-entriesSeparator");
		ret.setJustifyContentMode(JustifyContentMode.CENTER);
		return createNonLoginComponent(specEntry, ret);
	}

	private ComponentWithId getOptionHeader(String specEntry)
	{
		String key = specEntry.substring(SPECIAL_ENTRY_HEADER.length());
		
		String message = key.isEmpty() ? "" : resolveSeparatorMessage(key.substring(1));
		AuthNGridTextWrapper ret = new AuthNGridTextWrapper(new Span(message), Alignment.CENTER);
		ret.setClassName("u-authn-entryHeader");
		return createNonLoginComponent(specEntry, ret);
	}

	private String resolveSeparatorMessage(String key)
	{
		String value = config.getLocalizedValue(AUTHN_OPTION_LABEL_PFX + key + "." 
				+ AUTHN_OPTION_LABEL_TEXT, msg.getLocale());
		return value == null ? "" : value;
	}

	private ComponentWithId getGrid(String specEntry)
	{
		String key = specEntry.substring(SPECIAL_ENTRY_GRID.length());
		if (key.length() == 0)
			return null;
		String contents = config.getValue(AUTHN_GRIDS_PFX + key + "." + AUTHN_GRID_CONTENTS);
		if (contents == null)
			return null;
		int height = config.getIntValue(AUTHN_GRIDS_PFX + key + "." + AUTHN_GRID_ROWS);
		
		List<AuthNOption> options = getGridContents(contents);
		AuthnsGridWidget grid = new AuthnsGridWidget(options, msg, authNPanelFactory, height);
		return new ComponentWithId(specEntry, grid, options.size(), grid::getAuthnOptionById);
	}
	
	private List<AuthNOption> getGridContents(String contents)
	{
		List<AuthNOption> options = new ArrayList<>();
		String[] specSplit = contents.split("[ ]+");
		for (String specEntry: specSplit)
			options.addAll(authnOptionsHandler.getMatchingAuthnOptions(specEntry));
		return options;
	}

	private Component getColumnsSeparator(String columnKey)
	{
		String separator = config.getLocalizedValue(columnKey+AUTHN_COLUMN_SEPARATOR, msg.getLocale());
		if (separator == null || separator.isEmpty())
			separator = "";
		Span separatorLabel = new Span(separator);
		separatorLabel.addClassName("u-authn-columnsSeparator");
		separatorLabel.getStyle().set("margin-top", "4.5em");
		return separatorLabel;
	}
	
	private boolean entryIsText(String entry)
	{
		return entry != null && (entry.startsWith(SPECIAL_ENTRY_SEPARATOR) 
				|| entry.startsWith(SPECIAL_ENTRY_HEADER));
	}
	
	private Button buildRegistrationButton()
	{
		if (!enableRegistration)
			return null;
		Button register = new Button(msg.getMessage("RegistrationFormChooserDialog.register"));
		register.addClassName("u-signUpButton");
		register.addClickListener(event -> registrationLayoutLauncher.run());
		register.setId("AuthenticationUI.registerButton");
		register.setWidthFull();
		return register;
	}
}
