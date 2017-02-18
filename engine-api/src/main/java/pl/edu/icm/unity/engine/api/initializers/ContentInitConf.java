/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.initializers;

import java.io.File;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Corresponds to {@link UnityServerConfiguration#CONTENT_INITIALIZERS}
 * configuration entry in unityServer.conf.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class ContentInitConf
{
	private InitializerType type;
	private File file;

	public InitializerType getType()
	{
		return type;
	}

	public void setType(InitializerType type)
	{
		this.type = type;
	}

	public File getFile()
	{
		return file;
	}

	public void setFile(File file)
	{
		this.file = file;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@Override
	public String toString()
	{
		return "ContentInitConf [type=" + type + ", file=" + file + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContentInitConf other = (ContentInitConf) obj;
		if (file == null)
		{
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	public static class Builder
	{
		private InitializerType type;
		private File file;

		public Builder withType(InitializerType type)
		{
			this.type = type;
			return this;
		}

		public Builder withFile(File file)
		{
			this.file = file;
			return this;
		}

		public Builder withGroovy()
		{
			this.type = InitializerType.GROOVY;
			return this;
		}

		public ContentInitConf build()
		{
			ContentInitConf conf = new ContentInitConf();
			conf.setFile(file);
			conf.setType(type);
			return conf;
		}

	}
}
