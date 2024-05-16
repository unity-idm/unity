/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.layout;

import java.io.File;
import java.util.Optional;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

@Component
public class ExtraPanelsConfiguration
{
	private final UnityServerConfiguration unityServerConfiguration;
	
	public ExtraPanelsConfiguration(UnityServerConfiguration unityServerConfiguration)
	{
		this.unityServerConfiguration = unityServerConfiguration;
	}
	
	
	public Optional<File> getExtraLeftPanel()
	{
		return getFile(UnityServerConfiguration.EXTRA_LEFT_PANEL);
	}

	public Optional<File> getExtraRightPanel()
	{
		return getFile(UnityServerConfiguration.EXTRA_RIGHT_PANEL);
	}

	public Optional<File> getExtraTopPanel()
	{
		return getFile(UnityServerConfiguration.EXTRA_TOP_PANEL);
	}

	public Optional<File> getExtraBottomPanel()
	{
		return getFile(UnityServerConfiguration.EXTRA_BOTTOM_PANEL);
	}
	
	private Optional<File> getFile(String key)
	{
		if(unityServerConfiguration.getValue(key).isBlank())
			return Optional.empty();
		String value = unityServerConfiguration.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH) + "/" + unityServerConfiguration.getValue(key);
		return Optional.of(new File(value));
	}
}
