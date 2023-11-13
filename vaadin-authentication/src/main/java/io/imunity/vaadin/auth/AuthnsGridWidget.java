/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

import static io.imunity.vaadin.auth.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * Component showing a group of {@link VaadinAuthenticationUI}s. All of them are presented in Vaadin {@link Grid}.
 */
public class AuthnsGridWidget extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthnsGridWidget.class);
	private final AuthNPanelFactory authNPanelFactory;
	private final List<AuthNOption> options;
	private Map<String, VaadinAuthenticationUI> authenticatorById;
	private Grid<AuthenticationOptionGridEntry> providersChoiceGrid;
	private final Collator collator;
	private List<AuthenticationOptionGridEntry> providers;
	private final int height;
	
	public AuthnsGridWidget(List<AuthNOption> options, MessageSource msg,
	                        AuthNPanelFactory authNPanelFactory, int height)
	{
		this.options = options;
		this.authNPanelFactory = authNPanelFactory;
		this.height = height;
		collator = Collator.getInstance(msg.getLocale());
		initUI();
	}

	private void initUI()
	{
		setMargin(false);
		setPadding(false);

		providers = new ArrayList<>();
		providersChoiceGrid = new Grid<>();
		providersChoiceGrid.setSelectionMode(Grid.SelectionMode.NONE);
		providersChoiceGrid.addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS);


		providersChoiceGrid.addComponentColumn(AuthenticationOptionGridEntry::getImage)
				.setFlexGrow(1)
				.setAutoWidth(true);
		providersChoiceGrid.addComponentColumn(AuthenticationOptionGridEntry::getComponent)
				.setFlexGrow(6)
				.setAutoWidth(false);

		providersChoiceGrid.addClassName("u-authnGrid");
		providersChoiceGrid.setSizeFull();
		providersChoiceGrid.setClassNameGenerator(item -> "idpentry_" +
					AuthenticationOptionKeyUtils.encodeToCSS(item.getId()));
		providersChoiceGrid.setSizeUndefined();
		providersChoiceGrid.setAllRowsVisible(true);
		providersChoiceGrid.getStyle().set("margin", "var(--unity-auth-component-margin) 0");
		providersChoiceGrid.getStyle().set("height",
			"calc(" + height + "* (var(--unity-auth-component-height) + var(--unity-auth-full-component-margin)) - var(--unity-auth-full-component-margin))");

		reloadContents();
		add(providersChoiceGrid);
		setWidthFull();
	}

	/**
	 * Shows all authN UIs of all enabled authN options. The options not matching the given filter are 
	 * added too, but at the end and are hidden. This trick guarantees that the containing box 
	 * stays with a fixed size, while the user only sees the matching options at the top.
	 */
	private void reloadContents()
	{
		providers.clear();
		Map<String, AuthenticationFlow> authNOptionsById = new HashMap<>();
		authenticatorById = new HashMap<>();
		String firstOptionId = null;

		for (AuthNOption entry: options)
		{
			FirstFactorAuthNPanel authnPanel;
			try
			{
				authnPanel = authNPanelFactory.createGridCompatibleAuthnPanel(entry);
			} catch (UnsupportedOperationException e)
			{
				log.warn("Skipping {} option which is not grid compatible", entry.flow.getId());
				continue;
			}
			String name = entry.authenticatorUI.getLabel();
			Image logo = entry.authenticatorUI.getImage();
			String id = entry.authenticatorUI.getId();
			final String globalId = AuthenticationOptionKeyUtils.encode(
					entry.authenticator.getAuthenticatorId(), id);
			if (firstOptionId == null)
				firstOptionId = globalId;
			authNOptionsById.put(globalId, entry.flow);
			authenticatorById.put(globalId, entry.authenticatorUI);

			NameWithTags nameWithTags = new NameWithTags(name,
					entry.authenticatorUI.getTags(), collator);
			Image logoImage = logo == null ? new Image() : logo;
			logoImage.getStyle().set("max-height", "2.2rem");
			AuthenticationOptionGridEntry providerEntry = new AuthenticationOptionGridEntry(globalId, nameWithTags,
					logoImage, authnPanel);
			providers.add(providerEntry);
		}
		
		providers.sort(null);

		providersChoiceGrid.setItems(providers);

		setVisible(size() != 0);
	}
	
	private int size()
	{
		return authenticatorById.size();
	}
	
	public void filter(String filter)
	{
		providersChoiceGrid.setItems(providers.stream().filter(v -> v.getNameWithTags().contains(filter)).collect(Collectors.toList()));
	}

	public Optional<FirstFactorAuthNPanel> getAuthnOptionById(String optionId)
	{
		return providers.stream()
				.filter(prov -> prov.id.equals(optionId))
				.findAny()
				.map(entry -> entry.component);
	}
	
	public static class NameWithTags implements Comparable<Object>
	{
		private final String name;
		private final Set<String> tags;
		private final Collator collator;

		public NameWithTags(String name, Set<String> tags, Collator collator)
		{
			this.name = name;
			this.tags = tags;
			this.collator = collator;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public Set<String> getTags()
		{
			return tags;
		}

		public boolean contains(String what)
		{
			String whatLowercase = what.toLowerCase();
			if (name.toLowerCase().contains(whatLowercase))
				return true;
			for (String tag : tags)
				if (tag.toLowerCase().contains(whatLowercase))
					return true;
			return false;
		}

		@Override
		public int compareTo(Object o)
		{
			return collator.compare(toString(), o.toString());
		}
	}

	private static class AuthenticationOptionGridEntry implements Comparable<AuthenticationOptionGridEntry>
	{
		private final String id;
		private final NameWithTags nameWithTags;
		private final Component image;
		private final FirstFactorAuthNPanel component;

		public AuthenticationOptionGridEntry(String id, NameWithTags nameWithTags, Image image,
				FirstFactorAuthNPanel component)
		{
			this.id = id;
			this.nameWithTags = nameWithTags;
			this.image = image;
			this.component = component;
		}

		public String getId()
		{
			return id;
		}

		public NameWithTags getNameWithTags()
		{
			return nameWithTags;
		}

		public Component getImage()
		{
			VerticalLayout verticalLayout = new VerticalLayout(image);
			verticalLayout.setMargin(false);
			verticalLayout.setPadding(false);
			verticalLayout.setAlignItems(Alignment.CENTER);
			verticalLayout.setJustifyContentMode(JustifyContentMode.CENTER);
			return verticalLayout;
		}

		public Component getComponent()
		{
			if(component == null)
				return new Div();
			return component;
		}

		@Override
		public int compareTo(AuthenticationOptionGridEntry o)
		{
			String otherName = o.getNameWithTags().name;
			String thisName = getNameWithTags().name;
			return thisName.compareToIgnoreCase(otherName);
		}
	}
}
