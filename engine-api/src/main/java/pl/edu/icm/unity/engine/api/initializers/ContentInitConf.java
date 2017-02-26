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
	private InitializationPhase phase;
	private File file;
	
	private ContentInitConf() {}
	
	public InitializationPhase getPhase()
	{
		return phase;
	}

	public void setPhase(InitializationPhase phase)
	{
		this.phase = phase;
	}

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
		return "ContentInitConf [type=" + type + ", phase=" + phase + ", file=" + file + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + ((phase == null) ? 0 : phase.hashCode());
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
		if (phase != other.phase)
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	public static class Builder
	{
		private InitializerType type;
		private InitializationPhase phase;
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
		
		public Builder withPhase(InitializationPhase phase)
		{
			this.phase = phase;
			return this;
		}

		public Builder withGroovy()
		{
			this.type = InitializerType.GROOVY;
			return this;
		}

		public ContentInitConf build()
		{
			if (file == null)
				throw new IllegalArgumentException("file must not be null");
			if (type == null)
				throw new IllegalArgumentException("type must not be null");
			if (phase == null)
				throw new IllegalArgumentException("phase must not be null");
			ContentInitConf conf = new ContentInitConf();
			conf.setFile(file);
			conf.setType(type);
			conf.setPhase(phase);
			return conf;
		}

	}
}
