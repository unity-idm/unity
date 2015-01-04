package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.api.confirmations.ConfirmationFaciliity;

public class ConfirmationFacilitiesRegistry extends TypesRegistryBase<ConfirmationFaciliity>
{
	
	@Autowired
	public ConfirmationFacilitiesRegistry(List<ConfirmationFaciliity> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(ConfirmationFaciliity from)
	{
		return from.getName();
	}

}
