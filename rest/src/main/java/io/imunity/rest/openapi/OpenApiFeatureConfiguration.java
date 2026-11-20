package io.imunity.rest.openapi;

import org.apache.cxf.jaxrs.openapi.OpenApiFeature;

/** Opt-in endpoint configuration; ordinary REST applications retain the shared defaults. */
public interface OpenApiFeatureConfiguration
{
	void configureOpenApi(OpenApiFeature feature);
}
