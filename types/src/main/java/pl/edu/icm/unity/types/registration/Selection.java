/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Base class of option selection, actually binary.
 * If the selection is associated 
 * @author K. Benedyczak
 */
public class Selection
{
	private boolean selected;
	private String externalIdp;

	public Selection()
	{
	}

	public Selection(boolean selected)
	{
		this.selected = selected;
	}

	public Selection(boolean selected, String externalIdp)
	{
		this.selected = selected;
		this.externalIdp = externalIdp;
	}

	public boolean isSelected()
	{
		return selected;
	}

	public void setSelected(boolean selected)
	{
		this.selected = selected;
	}

	public String getExternalIdp()
	{
		return externalIdp;
	}

	public void setExternalIdp(String externalIdp)
	{
		this.externalIdp = externalIdp;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((externalIdp == null) ? 0 : externalIdp.hashCode());
		result = prime * result + (selected ? 1231 : 1237);
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
		Selection other = (Selection) obj;
		if (externalIdp == null)
		{
			if (other.externalIdp != null)
				return false;
		} else if (!externalIdp.equals(other.externalIdp))
			return false;
		if (selected != other.selected)
			return false;
		return true;
	}
}
