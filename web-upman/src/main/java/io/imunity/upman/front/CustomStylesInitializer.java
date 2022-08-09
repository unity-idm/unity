/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front;

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
import pl.edu.icm.unity.webui.VaadinEndpointProperties;

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
		VaadinEndpointProperties currentWebAppVaadinProperties = getCurrentWebAppVaadinProperties();
		File externalCSSResource = currentWebAppVaadinProperties.getCustomCssFile();
		File oldVaadin8CSSResource = currentWebAppVaadinProperties.getOldVaadin8CssFile();

		serviceInitEvent.addIndexHtmlRequestListener(new CustomStylesInjector(externalCSSResource, oldVaadin8CSSResource));
	}

	private static class CustomStylesInjector implements IndexHtmlRequestListener
	{

		private final CustomStylesContentProvider contentProvider;

		CustomStylesInjector(File externalCSSResource, File oldVaadin8CSSResource)
		{
			this.contentProvider = new CustomStylesContentProvider(oldVaadin8CSSResource, externalCSSResource);
		}

		@Override
		public void modifyIndexHtmlResponse(IndexHtmlResponse indexHtmlResponse)
		{
//			contentProvider.getCustomStyles().ifPresent(customStyles -> {
//				applyStyle(indexHtmlResponse, customStyles, "custom-style");
//			});
//			contentProvider.getOldVaadin8Styles().ifPresent(customStyles -> {
//				applyStyle(indexHtmlResponse, customStyles, "old-style");
//			});
		}

		private void applyStyle(IndexHtmlResponse indexHtmlResponse, String customStyles, String styleName)
		{
			Document document = indexHtmlResponse.getDocument();
			Element head = document.head();
			head.appendChild(createStyle(document, customStyles, styleName));
		}

		private Element createStyle(Document document, String customStyles, String styleName)
		{
			Element customStyle = document.createElement(styleName);
			Element style = document.createElement("style");
			customStyle.appendChild(style);
			style.appendText(customStyles);
			return customStyle;
		}
	}

	private static class CustomStylesContentProvider {

		private final File externalCSSResource;
		private final File oldVaadin8CSSResource;

		CustomStylesContentProvider(File externalCSSResource, File oldVaadin8CSSResource) {
			this.externalCSSResource = externalCSSResource;
			this.oldVaadin8CSSResource = oldVaadin8CSSResource;
		}

		private Optional<String> getCustomStyles() {
			return getStyle(externalCSSResource);
		}

		private Optional<String> getOldVaadin8Styles() {
			return getStyle(oldVaadin8CSSResource);
		}

		private Optional<String> getStyle(File style)
		{
			if (isCustomCssFileAvailable()) {
				String msg = null;
				try {
					msg = StreamUtils.copyToString(new FileInputStream(style), Charset.defaultCharset());
				} catch (IOException exception) {
					LOG.error(format("Could not read custom CSS file: %s", style.getName()),
							exception);
				}
				return Optional.ofNullable(msg);
			}
			return Optional.empty();
		}

		private boolean isOldVaadin8CssFileAvailable()
		{
			return isStyleAvailable(oldVaadin8CSSResource);
		}
		private boolean isCustomCssFileAvailable()
		{
			return isStyleAvailable(externalCSSResource);
		}

		private boolean isStyleAvailable(File style)
		{
			if (style == null) {
				LOG.debug("Custom style is not configured.", style);
				return false;
			}

			if (!style.exists()) {
				LOG.error("Could not load custom styles: file does not exists, {}.", style);
				return false;
			}

			if (!style.isFile()) {
				LOG.error("Could not load custom styles: unable to read file content, {}.", style);
				return false;
			}

			return true;
		}
	}
}
