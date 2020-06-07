/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sessionscope;

import java.util.Objects;

@WebSessionComponent
class WebSessionTestBean
{
	private static int counter = 0;
	
	String value;

	WebSessionTestBean()
	{
		this.value = String.valueOf(counter++);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(value);
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
		WebSessionTestBean other = (WebSessionTestBean) obj;
		return Objects.equals(value, other.value);
	}
}
