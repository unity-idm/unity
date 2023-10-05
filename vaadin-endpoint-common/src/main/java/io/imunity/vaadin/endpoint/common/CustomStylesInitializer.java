/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.Optional;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppVaadinProperties;
import static java.lang.String.format;

class CustomStylesInitializer implements VaadinServiceInitListener
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final DefaultCssFileLoader defaultCssFileLoader;

	CustomStylesInitializer(DefaultCssFileLoader defaultCssFileLoader)
	{
		this.defaultCssFileLoader = defaultCssFileLoader;
	}

	@Override
	public void serviceInit(ServiceInitEvent serviceInitEvent)
	{
		Vaadin82XEndpointProperties currentWebAppVaadinProperties = getCurrentWebAppVaadinProperties();

		String customCssFilePath = currentWebAppVaadinProperties.getCustomCssFilePath().orElse(null);
		CssFileLoader customCssFileLoader = new CssFileLoader(customCssFilePath);

		serviceInitEvent.addIndexHtmlRequestListener(new CustomStylesInjector(defaultCssFileLoader.get(), customCssFileLoader));
	}

	private static class CustomStylesInjector implements IndexHtmlRequestListener
	{

		private final CustomStylesContentProvider contentProvider;

		CustomStylesInjector(CssFileLoader defaultCssFileLoader, CssFileLoader externalCssFileLoader)
		{
			this.contentProvider = new CustomStylesContentProvider(defaultCssFileLoader, externalCssFileLoader);
		}

		@Override
		public void modifyIndexHtmlResponse(IndexHtmlResponse indexHtmlResponse)
		{
			contentProvider.getDefaultStyles().ifPresent(customStyles ->
			{
				Document document = indexHtmlResponse.getDocument();
				org.jsoup.nodes.Element head = document.head();
				head.appendChild(createStyle(document, customStyles));
			});
			contentProvider.getCustomStyles().ifPresent(customStyles ->
			{
				Document document = indexHtmlResponse.getDocument();
				org.jsoup.nodes.Element head = document.head();
				head.appendChild(createStyle(document, customStyles));
			});
		}

		private Element createStyle(Document document, String customStyles)
		{
			Element customStyle = document.createElement("custom-style");
			Element style = document.createElement("style");
			customStyle.appendChild(style);
			style.append(customStyles);
			return customStyle;
		}
	}

	private static class CustomStylesContentProvider
	{

		private final CssFileLoader defaultCssFileLoader;
		private final CssFileLoader customCssFileLoader;

		CustomStylesContentProvider(CssFileLoader defaultCssFileLoader, CssFileLoader customCssFileLoader)
		{
			this.defaultCssFileLoader = defaultCssFileLoader;
			this.customCssFileLoader = customCssFileLoader;
		}

		private Optional<String> getCustomStyles()
		{
			File cssFile = customCssFileLoader.getCssFile();
			if (isCssFileAvailable(cssFile, "custom"))
			{
				return getContentAsString(cssFile);
			}
			return Optional.empty();
		}

		private Optional<String> getDefaultStyles()
		{
			File cssFile = defaultCssFileLoader.getCssFile();
			if (isCssFileAvailable(cssFile, "default"))
			{
				return getContentAsString(cssFile);
			}
			return Optional.empty();
		}

		private Optional<String> getContentAsString(File cssFile)
		{
			String msg = null;
			try
			{
				msg = StreamUtils.copyToString(new FileInputStream(cssFile), Charset.defaultCharset());
			} catch (IOException exception) {
				LOG.error(format("Could not read custom CSS file: %s", cssFile.getName()),
						exception);
			}
			return Optional.ofNullable(msg);
		}

		private boolean isCssFileAvailable(File cssFile, String styleType)
		{
			if (cssFile == null)
			{
				LOG.debug("{} style is not configured.", styleType);
				return false;
			}

			if (!cssFile.exists())
			{
				LOG.error("Could not load {} styles: file does not exists, {}.", cssFile, styleType);
				return false;
			}

			if (!cssFile.isFile())
			{
				LOG.error("Could not load {} styles: unable to read file content, {}.", cssFile, styleType);
				return false;
			}

			return true;
		}
	}
}
