/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.Optional;

import static io.imunity.vaadin23.endpoint.common.Vaadin23WebAppContext.getCurrentWebAppVaadinProperties;
import static java.lang.String.format;

@Component
class CustomStylesInitializer implements VaadinServiceInitListener
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void serviceInit(ServiceInitEvent serviceInitEvent)
	{
		Vaadin823EndpointProperties currentWebAppVaadinProperties = getCurrentWebAppVaadinProperties();
		File externalCSSResource = currentWebAppVaadinProperties.getCustomCssFile();

		serviceInitEvent.addIndexHtmlRequestListener(new CustomStylesInjector(externalCSSResource));
	}

	private static class CustomStylesInjector implements IndexHtmlRequestListener
	{

		private final CustomStylesContentProvider contentProvider;

		CustomStylesInjector(File externalCSSFile)
		{
			this.contentProvider = new CustomStylesContentProvider(externalCSSFile);
		}

		@Override
		public void modifyIndexHtmlResponse(IndexHtmlResponse indexHtmlResponse)
		{
			contentProvider.getCustomStyles().ifPresent(customStyles ->
			{
				Document document = indexHtmlResponse.getDocument();
				org.jsoup.nodes.Element head = document.head();
				head.appendChild(createCustomStyle(document, customStyles));
			});
		}

		private Element createCustomStyle(Document document, String customStyles)
		{
			Element customStyle = document.createElement("custom-style");
			Element style = document.createElement("style");
			customStyle.appendChild(style);
			style.appendText(customStyles);
			return customStyle;
		}
	}

	private static class CustomStylesContentProvider
	{

		private final File externalCSSFile;

		CustomStylesContentProvider(File externalCSSFile)
		{
			this.externalCSSFile = externalCSSFile;
		}

		private Optional<String> getCustomStyles()
		{

			if (isCustomCssFileAvailable())
			{
				String msg = null;
				try
				{
					msg = StreamUtils.copyToString(new FileInputStream(externalCSSFile), Charset.defaultCharset());
				} catch (IOException exception) {
					LOG.error(format("Could not read custom CSS file: %s", externalCSSFile.getName()),
							exception);
				}
				return Optional.ofNullable(msg);
			}
			return Optional.empty();
		}

		private boolean isCustomCssFileAvailable()
		{

			if (externalCSSFile == null)
			{
				LOG.debug("Custom style is not configured.");
				return false;
			}

			if (!externalCSSFile.exists())
			{
				LOG.error("Could not load custom styles: file does not exists, {}.", externalCSSFile);
				return false;
			}

			if (!externalCSSFile.isFile())
			{
				LOG.error("Could not load custom styles: unable to read file content, {}.", externalCSSFile);
				return false;
			}

			return true;
		}
	}
}
