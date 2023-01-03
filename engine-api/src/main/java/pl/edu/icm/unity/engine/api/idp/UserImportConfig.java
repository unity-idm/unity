/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import java.util.Objects;

public class UserImportConfig
{
	public final String key;
	public final String importer;
	public final String type;

	public UserImportConfig(String key, String importer, String type)
	{
		this.key = key;
		this.importer = importer;
		this.type = type;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserImportConfig that = (UserImportConfig) o;
		return Objects.equals(key, that.key) && Objects.equals(importer, that.importer) && Objects.equals(type, that.type);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key, importer, type);
	}

	@Override
	public String toString()
	{
		return "UserImportConfig{" +
				"key='" + key + '\'' +
				", importer='" + importer + '\'' +
				", type='" + type + '\'' +
				'}';
	}
}
