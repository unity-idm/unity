# unity
This is a fork of the principal repository of Unity IdM service: https://github.com/unity-idm/unity

This fork is the home for:
1. Development of all CLARIN related LDAP requirements for unity-idm. 
1. Development of specific CLARIN related customizations.

# Contributing

Currently a unity-idm 1.9.6 instance is deployed in production. There are two main branches:

1. ldapEndpoint - this is a 1.9.5 branch which is used to create pull request into the upstream unity-idm repository in order 
to contribute our changes back into the upstream repository.
2. ldapEndpoint-clarin-1.9.6 - this is the branch where we develop our customization for our production deployment. These 
changes are usually not contributed back into the upstream repository.

## Contributing code / new features

When developing new ldap features, please create a new feature branch based on the ldapEndpoint branch and create a pull 
after finished the new feature.

When developinf a new CLARIN specific feature, create a new feature branch based on the ldapEndpoint-clarin-1.9.6 and create
a pull request after finishing the feature.

