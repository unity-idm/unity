package pl.edu.icm.unity.saml.metadata.cfg;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FederationIdPsFilterContextKey
{
	
	entityID(FederationIdPsFilterContextKey.descriptionPrefix + "entityID"),
	attributes(FederationIdPsFilterContextKey.descriptionPrefix + "attributes");

	public static final String descriptionPrefix = "FederationIdpsFilterMVELContextKey.";
	public final String descriptionKey;

	
	private FederationIdPsFilterContextKey(String descriptionKey)
	{
		this.descriptionKey = descriptionKey;
	}

	public static Map<String, String> toMap()
	{
		return Stream.of(values())
				.collect(Collectors.toMap(v -> v.name(), v -> v.descriptionKey));
	}
}
