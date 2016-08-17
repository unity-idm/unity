/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.initializers;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Corresponds to {@link UnityServerConfiguration#CONTENT_INITIALIZERS}
 * configuration entry in unityServer.conf.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class ContentInitConf
{
	private InitializerMode mode;
	private InitializerType type;
	private String file;

	public InitializerMode getMode()
	{
		return mode;
	}

	public void setMode(InitializerMode mode)
	{
		this.mode = mode;
	}

	public InitializerType getType()
	{
		return type;
	}

	public void setType(InitializerType type)
	{
		this.type = type;
	}

	public String getFile()
	{
		return file;
	}

	public void setFile(String file)
	{
		this.file = file;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		private InitializerMode mode;
		private InitializerType type;
		private String file;

		public Builder withMode(InitializerMode mode)
		{
			this.mode = mode;
			return this;
		}

		public Builder withType(InitializerType type)
		{
			this.type = type;
			return this;
		}

		public Builder withFile(String file)
		{
			this.file = file;
			return this;
		}

		public ContentInitConf build()
		{
			ContentInitConf conf = new ContentInitConf();
			conf.setFile(file);
			conf.setType(type);
			conf.setMode(mode);
			return conf;
		}

	}

}
