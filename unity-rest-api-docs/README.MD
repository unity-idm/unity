# Install Redocly CLI follow https://redocly.com/docs/redoc/deployment/cli/

# Compile all projects: 
	cd <unity-idm-repo>
	mvn install -DskipTests
	
# Generate Open API spec in yaml format:
	cd <unity-idm-repo>/unity-rest-api-docs
	mvn clean install -PopenapiSpec
	
The spec will be available in <unity-idm-repo>/unity-rest-api-docs/target/openapi.yaml

# Generate HTML doc out of schema
	cd <unity-idm-repo>/unity-rest-api-docs/target
	redocly build-docs --theme.openapi.hideHostname=true openapi.yaml
	mv redoc-static.html unity-openapi-static.html
	
Generated documentation can be reviewed in unity-openapi-static.html