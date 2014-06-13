/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.idpselector;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import javax.servlet.http.Cookie;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.CookieHelper;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileResource;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.Reindeer;

/**
 * IDP selector component.
 * @author K. Benedyczak
 */
public class IdpSelectorComponent extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, IdpSelectorComponent.class);
	
	public enum ScaleMode {none, height100, width100, height50, width50}
	
	private UnityMessageSource msg;
	private int perRow;
	private ScaleMode scaleMode;
	private IdPsSpecification idps;
	private String lastIdpCookie;
	
	private Panel idpsPanel;
	private String selectedProvider;
	private Button selectedButton;

	public IdpSelectorComponent(UnityMessageSource msg, int perRow, ScaleMode scaleMode, 
			String lastIdpCookie, IdPsSpecification idps)
	{
		this.msg = msg;
		this.perRow = perRow;
		this.idps = idps;
		this.lastIdpCookie = lastIdpCookie;
		this.scaleMode = scaleMode;
		initUI();
	}

	public String getSelectedProvider()
	{
		return selectedProvider;
	}
	
	public static void setLastIdpCookie(String name, String idpKey)
	{
		VaadinResponse resp = VaadinService.getCurrentResponse();
		Cookie selectedIdp = new Cookie(name, idpKey);
		selectedIdp.setMaxAge(3600*24*30);
		selectedIdp.setPath("/");
		resp.addCookie(selectedIdp);
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		setCompositionRoot(main);
		
		Component previous = initPrevousIdp();
		if (previous != null)
		{
			Panel previousIdpPanel = new Panel();
			previousIdpPanel.addStyleName(Styles.contentPadRight20.toString());
			previousIdpPanel.setContent(previous);
			main.addComponent(previousIdpPanel);
			main.addComponent(new Label(""));
		}
		
		if (idps.getIdpKeys().size() > 9)
		{
			FormLayout wrapper = new FormLayout();
			TextField search = new TextField(msg.getMessage("IdpSelectorComponent.search"));
			search.addStyleName(Reindeer.TEXTFIELD_SMALL);
			search.setImmediate(true);
			wrapper.addComponent(search);
			main.addComponent(wrapper);
			search.addTextChangeListener(new TextChangeListener()
			{
				@Override
				public void textChange(TextChangeEvent event)
				{
					idpsPanel.setContent(initIdpsList(event.getText()));
				}
			});
		}
		idpsPanel = new Panel();
		idpsPanel.addStyleName(Styles.contentPadRight20.toString());
		main.addComponents(idpsPanel);
		idpsPanel.setContent(initIdpsList(null));
	}
	
	private Component initPrevousIdp()
	{
		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		hl.setMargin(true);
		VaadinRequest req = VaadinService.getCurrentRequest();
		if (req == null)
			return null;
		String lastIdp = CookieHelper.getCookie(req.getCookies(), lastIdpCookie);
		if (lastIdp == null)
			return null;
		Button providerB;
		try
		{
			providerB = createProviderButton(lastIdp, msg.getLocale());
		} catch (IllegalArgumentException e)
		{
			return null;
		}
		Label info = new Label(msg.getMessage("IdpSelectorComponent.last"));
		hl.addComponents(info, providerB);
		hl.setComponentAlignment(info, Alignment.MIDDLE_RIGHT);
		return hl;
	}
	
	private GridLayout initIdpsList(String filter)
	{
		if (filter != null && filter.trim().equals(""))
			filter = null;
		if (filter != null)
			filter = filter.toLowerCase();
		GridLayout providersChoice = new GridLayout(perRow, 1);
		providersChoice.setSpacing(true);
		providersChoice.addStyleName(Styles.maxHeight300.toString());

		int current = 0;
		Locale locale = msg.getLocale();
		for (String idpKey: idps.getIdpKeys())
		{			
			String name = idps.getIdPName(idpKey, locale);
			if (filter != null && !name.toLowerCase().contains(filter))
				continue;
			Button providerB = createProviderButton(idpKey, locale);
			
			providersChoice.addComponent(providerB);
			providersChoice.setComponentAlignment(providerB, Alignment.MIDDLE_LEFT);

			if (current == 0)
			{
				selectedProvider = idpKey;
				selectedButton = providerB;
				selectedButton.addStyleName(Styles.selectedButton.toString());
			}

			current++;
		}
		return providersChoice;
	}
	
	private Button createProviderButton(String idpKey, Locale locale)
	{
		String name = idps.getIdPName(idpKey, locale);
		Button providerB = new Button();
		providerB.setImmediate(true);
		providerB.setStyleName(Reindeer.BUTTON_LINK);
		providerB.addStyleName(Styles.verticalMargins6.toString());
		providerB.addStyleName(Styles.horizontalMargins6.toString());

		Resource logo = getLogoResource(idpKey, locale);
		if (logo == null && name == null)
		{
			throw new IllegalArgumentException(idpKey + " is unknown");
		}
		if (logo != null)
		{
			providerB.setIcon(logo);
			switch (scaleMode)
			{
			case width100:
				providerB.addStyleName(Styles.width100.toString());
				break;
			case height100:
				providerB.addStyleName(Styles.height100.toString());
				break;
			case width50:
				providerB.addStyleName(Styles.width50.toString());
				break;
			case height50:
				providerB.addStyleName(Styles.height50.toString());
				break;
			case none:
			}
			providerB.setDescription(name);
		} else
		{
			providerB.setCaption(name);
		}
		providerB.setData(idpKey);
		providerB.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				selectedProvider = (String) event.getButton().getData();
				selectedButton.removeStyleName(Styles.selectedButton.toString());
				event.getButton().addStyleName(Styles.selectedButton.toString());
				selectedButton = event.getButton();
			}
		});
		return providerB;
	}
	
	private Resource getLogoResource(String idpKey, Locale locale)
	{
		String uri = idps.getIdPLogoUri(idpKey, locale);
		if (uri == null)
			return null;
		if (uri.startsWith("http:") || uri.startsWith("https:") || uri.startsWith("data:"))
			return new ExternalResource(uri);
		if (uri.startsWith("file:"))
		{
			URL url;
			try
			{
				url = new URL(uri);
			} catch (MalformedURLException e)
			{
				log.warn("Invalid file URI of logo: " + uri, e);
				return null;
			}
			String path = url.getPath();
			return new FileResource(new File(path));
		}
		log.warn("Unsupported logo URI scheme: " + uri);
		return null;
	}
}
