/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_ADD_ALL;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMNS_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_SEPARATOR;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_TITLE;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_WIDTH;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRIDS_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRID_CONTENTS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRID_ROWS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_SHOW_SEARCH;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthNGridTextWrapper;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.column.AuthnOptionsColumn.ComponentWithId;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Core component maintaining set of columns with authentication options.
 * Allow for general operations like disabling or hiding.
 * 
 * @author K. Benedyczak
 */
public class AuthnOptionsColumns extends CustomComponent
{
	private static final String SPECIAL_ENTRY_LAST_USED = "_LAST_USED";
	private static final String SPECIAL_ENTRY_REGISTER = "_REGISTER";
	private static final String SPECIAL_ENTRY_SEPARATOR = "_SEPARATOR";
	private static final String SPECIAL_ENTRY_HEADER = "_HEADER";
	private static final String SPECIAL_ENTRY_GRID = "_GRID_";
	private static final String SPECIAL_ENTRY_EXPAND = "_EXPAND"; //note that this one is not documented, for internal use
	
	private final VaadinEndpointProperties config;
	private final UnityMessageSource msg;
	private final AuthenticationOptionsHandler authnOptionsHandler;
	private final boolean enableRegistration;
	private final AuthNPanelFactory authNPanelFactory;
	private final Runnable registrationLayoutLauncher;
	
	private List<AuthnOptionsColumn> columns;
	
	AuthnOptionsColumns(VaadinEndpointProperties config, UnityMessageSource msg,
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
		setWidthUndefined();
		focusFirst();
	}

	void refreshAuthenticatorWithId(String id, VaadinRequest request)
	{
		for (AuthnOptionsColumn column: columns)
		{
			FirstFactorAuthNPanel ffAuthnPanel = column.getAuthnOptionById(id);
			if (ffAuthnPanel != null)
				ffAuthnPanel.refresh(request);
		}
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
		if (config.getBooleanValue(AUTHN_SHOW_SEARCH) && hasGridWidget())
		{
			VerticalLayout vWrapper = new VerticalLayout();
			vWrapper.setWidthUndefined();
			vWrapper.setMargin(false);
			SearchComponent search = new SearchComponent(msg, this::filter);
			vWrapper.addComponent(search);
			vWrapper.setComponentAlignment(search, Alignment.MIDDLE_RIGHT);
			
			vWrapper.addComponent(component);
			setCompositionRoot(vWrapper);
		} else
		{
			setCompositionRoot(component);
		}
	}


	private Component getAuthnColumnsComponent()
	{
		Component authNColumns = getFullAuthnColumnsComponent();
		if (config.getBooleanValue(AUTHN_SHOW_LAST_OPTION_ONLY) && 
				PreferredAuthenticationHelper.getPreferredIdp() != null &&
				hasMoreThenOneOptionConfigured())
		{
			authnOptionsHandler.clear();
			Component lastSelectionComponent = createLastSelectionLayout();
			if (lastSelectionComponent != null)
			{
				return lastSelectionComponent;
			}
		}
		return authNColumns;
	}

	private Component getFullAuthnColumnsComponent()
	{
		authnOptionsHandler.clear();
		Iterator<String> columnKeys = config.getStructuredListKeys(AUTHN_COLUMNS_PFX).iterator();
		if (!columnKeys.hasNext())
		{
			return createDefaultLayout();
		} else
		{
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
		columnsLayout.addComponent(columnComponent);
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
		columnsLayout.addComponent(columnComponent);
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
			columnsLayout.addComponent(columnComponent);
			columns.add(columnComponent);
			columnComponent.addOptions(columnAuthnComponents);
			
			if (!focussed)
				focussed = columnComponent.focusFirst();
			
			if (columnKeys.hasNext())
			{
				Component separator = getColumnsSeparator(columnKey);
				columnsLayout.addComponent(separator);
				columnsLayout.setComponentAlignment(separator, Alignment.MIDDLE_CENTER);
			}
		}
		return columnsLayout;
	}

	
	private List<ComponentWithId> getColumnAuthnComponents(String columnContents, boolean addRemaining)
	{
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
					ret.add(new ComponentWithId(specEntry, registrationButton));
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
						ret.add(new ComponentWithId(authNPanel.getAuthenticationOptionId(), authNPanel));
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
					ret.add(new ComponentWithId(authNPanel.getAuthenticationOptionId(), authNPanel));
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
				ret.add(new ComponentWithId(authNPanel.getAuthenticationOptionId(), authNPanel));
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
		
		return ret;
	}
	
	private ComponentWithId getExpandAllOptionsButton()
	{
		Button expand = new Button(msg.getMessage("AuthenticationUI.showAllOptions"));
		expand.addStyleName(Styles.vButtonLink.toString());
		expand.addClickListener(event -> showAllOptions());
		return new ComponentWithId(SPECIAL_ENTRY_EXPAND, expand);
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
		AuthNGridTextWrapper ret = new AuthNGridTextWrapper(new Label(message), Alignment.MIDDLE_CENTER);
		ret.setStyleName("u-authn-entriesSeparator");
		return new ComponentWithId(specEntry, ret);
	}

	private ComponentWithId getOptionHeader(String specEntry)
	{
		String key = specEntry.substring(SPECIAL_ENTRY_HEADER.length());
		
		String message = key.isEmpty() ? "" : resolveSeparatorMessage(key.substring(1));
		AuthNGridTextWrapper ret = new AuthNGridTextWrapper(new Label(message), Alignment.MIDDLE_CENTER);
		ret.setStyleName("u-authn-entryHeader");
		return new ComponentWithId(specEntry, ret);
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
		return new ComponentWithId(specEntry, grid);
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
		Label separatorLabel = new Label(separator);
		separatorLabel.setStyleName("u-authn-columnsSeparator");
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
		register.addStyleName("u-signUpButton");
		register.addClickListener(event -> registrationLayoutLauncher.run());
		register.setId("AuthenticationUI.registerButton");
		return register;
	}
}
