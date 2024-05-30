/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;

import pl.edu.icm.unity.base.utils.Log;

import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, MethodHandles.lookup().lookupClass());

	private final DefaultCssFileLoader defaultCssFileLoader;
	private final GlobalCustomCssFileLoader globalCustomCssLoader;

	CustomStylesInitializer(DefaultCssFileLoader defaultCssFileLoader, GlobalCustomCssFileLoader globalCustomCssLoader)
	{
		this.defaultCssFileLoader = defaultCssFileLoader;
		this.globalCustomCssLoader = globalCustomCssLoader;
	}

	@Override
	public void serviceInit(ServiceInitEvent serviceInitEvent)
	{
		VaadinEndpointProperties currentWebAppVaadinProperties = getCurrentWebAppVaadinProperties();

		String customCssFilePath = currentWebAppVaadinProperties.getCustomCssFilePath(defaultCssFileLoader.getDefaultWebConentPath()).orElse(null);
		CssFileLoader customCssFileLoader = new CssFileLoader(customCssFilePath);

		serviceInitEvent.addIndexHtmlRequestListener(new CustomStylesInjector(defaultCssFileLoader.get(),
				globalCustomCssLoader.get(), customCssFileLoader));
	}

	private static class CustomStylesInjector implements IndexHtmlRequestListener
	{
		private final CustomStylesContentProvider contentProvider;
		private final CssFileLoader defaultCssFileLoader;
		private final CssFileLoader globalCustomCssLoader;
		private final CssFileLoader endpointCustomCssLoader;

		CustomStylesInjector(CssFileLoader defaultCssFileLoader, CssFileLoader globalCustomCssLoader,
				CssFileLoader endpointCustomCssLoader)
		{
			this.defaultCssFileLoader = defaultCssFileLoader;
			this.globalCustomCssLoader = globalCustomCssLoader;
			this.endpointCustomCssLoader = endpointCustomCssLoader;
			this.contentProvider = new CustomStylesContentProvider();
		}

		@Override
		public void modifyIndexHtmlResponse(IndexHtmlResponse indexHtmlResponse)
		{
			injectIfPresent(indexHtmlResponse, 
					contentProvider.getStyle(defaultCssFileLoader.getCssFile(), "custom server"));
			injectIfPresent(indexHtmlResponse, 
					contentProvider.getStyle(globalCustomCssLoader.getCssFile(), "custom global"));
			injectIfPresent(indexHtmlResponse, 
					contentProvider.getStyle(endpointCustomCssLoader.getCssFile(), "custom endpoint"));
		}

		private void injectIfPresent(IndexHtmlResponse indexHtmlResponse, Optional<String> css)
		{
			css.ifPresent(customStyles ->
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
		private Optional<String> getStyle(File cssFile, String type)
		{
			return isCssFileAvailable(cssFile, type) ?
				getContentAsString(cssFile) : Optional.empty();
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
				LOG.debug("{} style is not configured", styleType);
				return false;
			}

			if (!cssFile.exists())
			{
				LOG.error("Could not load {} styles: file does not exist, {}", cssFile, styleType);
				return false;
			}

			if (!cssFile.isFile())
			{
				LOG.error("Could not load {} styles: unable to read file content, {}", cssFile, styleType);
				return false;
			}

			return true;
		}
	}
}
