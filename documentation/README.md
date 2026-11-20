# Documentation site deployment

After installing the reactor dependencies, generate the site from the repository root with Asciidoctor on `PATH`:

```sh
mvn -pl documentation compile site -Dgpg.skip=true
```

The `pre-site` executions generate the Asciidoctor pages and the Admin and UpMan OpenAPI references in
`${project.reporting.outputDirectory}` (`documentation/target/site` by default). The site plugin deploys this entire
directory recursively through the existing SCP Wagon to the versioned directory configured in
`documentation/pom.xml`: `unity-idm.eu/documentation/unity-${project.version}`. There is no separate HTML-only file
list or documentation archive. The generated reference therefore travels with the manual and narrative pages:

```text
manual.html
rest-api-v1.html
upman-rest-api-v1.html
admin-openapi-v1.html
admin-openapi-v1.yaml
upman-openapi-v1.html
upman-openapi-v1.yaml
openapi-assets/
```

Keep the relative paths intact: the generated HTML loads its offline renderer assets from the local `openapi-assets`
directory, and the site index links to the sibling HTML and YAML files. Transport the entire assets directory,
including any files added or renamed when the renderer changes.
The ordinary publication command is `mvn -pl documentation compile site-deploy -Dgpg.skip=true`; this uploads to the
configured remote site and requires the existing `unity-idm.eu` server credentials in Maven settings.

To check the deployment locally after generating the site, use a fresh local directory:

```sh
staged_site=$(mktemp -d /tmp/unity-documentation-stage.XXXXXX)
mvn -pl documentation site:stage-deploy -Dgpg.skip=true \
  -DstagingSiteURL="file://$staged_site" -Dmaven.site.chmod=false
diff -r documentation/target/site "$staged_site"
```

`site:stage-deploy` uses the same recursive Wagon upload as `site:deploy`, with a local file transport in this example.
The comparison checks every generated file and subdirectory, including the manual, OpenAPI HTML/YAML and all offline
renderer assets. The staging URL overrides the remote destination; disabling `chmod` is only needed for this local
check. Linked worktrees may also require `-Dmaven.gitcommitid.nativegit=true` on these Maven commands.
