/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.idpselector;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileResource;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
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
	
	private UnityMessageSource msg;
	private int perRow;
	private IdPsSpecification idps;
	
	private Panel idpsPanel; 
	private String selectedProvider;
	private Button selectedButton;

	public IdpSelectorComponent(UnityMessageSource msg, int perRow, IdPsSpecification idps)
	{
		this.msg = msg;
		this.perRow = perRow;
		this.idps = idps;
		initUI();
	}

	public String getSelectedProvider()
	{
		return selectedProvider;
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		setCompositionRoot(main);
		if (idps.getIdpKeys().size() > 9)
		{
			FormLayout wrapper = new FormLayout();
			TextField search = new TextField(msg.getMessage("IdpSelectorComponent.search"));
			search.setImmediate(true);
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
		main.addComponent(idpsPanel);
		idpsPanel.setContent(initIdpsList(null));
	}
	
	private VerticalLayout initIdpsList(String filter)
	{
		if (filter != null && filter.trim().equals(""))
			filter = null;
		if (filter != null)
			filter = filter.toLowerCase();
		VerticalLayout providersChoice = new VerticalLayout();
		providersChoice.setSpacing(true);

		int current = 0;
		HorizontalLayout providersL = null;
		Locale locale = msg.getLocale();
		for (String idpKey: idps.getIdpKeys())
		{
			if ((current % perRow) == 0)
			{
				providersL = new HorizontalLayout();
				providersL.setSpacing(true);
				providersChoice.addComponent(providersL);
				providersL.addStyleName(Styles.verticalMargins10.toString());
			}

			String name = idps.getIdPName(idpKey, locale);
			
			if (filter != null && !name.toLowerCase().contains(filter))
				continue;
			
			Button providerB = new Button();
			providerB.setImmediate(true);
			providerB.setStyleName(Reindeer.BUTTON_LINK);
			providerB.addStyleName(Styles.horizontalMargins10.toString());
			if (current == 0)
			{
				selectedProvider = idpKey;
				selectedButton = providerB;
				selectedButton.addStyleName(Styles.selectedButton.toString());
			}
			
			
			Resource logo = getLogoResource(idpKey, locale);
			if (logo != null)
			{
				providerB.setIcon(logo);
			} else
			{
				providerB.setCaption(name);
			}
			
			providersL.addComponent(providerB);
			providersL.setComponentAlignment(providerB, Alignment.MIDDLE_LEFT);
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
			current++;
		}
		return providersChoice;
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
	
	/*
	 * TODO - not needed? If so remove
	private Resource getDataURIAsResource(String dataURI)
	{
		//URI format:   data:[<MIME-type>][;charset=<encoding>][;base64],<data>
		int commaPos = dataURI.indexOf(',');
		String pfx = dataURI.substring(0, commaPos);
		String data = dataURI.substring(commaPos + 1);
		if (pfx.endsWith(";base64")) 
		{
			String mime = pfx.substring(5, pfx.indexOf(';'));
			
			byte[] bytes = Base64.decode(data);
			final ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
			return new StreamResource(new StreamSource()
			{
				@Override
				public InputStream getStream()
				{
					return stream;
				}
			}, );
		}
		log.warn("Logo data URI is not of base64 encoding, it is unsupported: " + pfx);
		return null;
	}
	*/
}
