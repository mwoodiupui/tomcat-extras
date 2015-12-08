# tomcat-extras
Additional bits and bobs for Apache Tomcat

The only thing provided right now is a Valve which restricts access based on the client's address.  Unlike the one that
comes with Tomcat, this one matches on a list of CIDR address/mask pairs rather than a single regular expression.
