# Use the Debian-based OpenJDK 21 image
FROM tomcat:9.0-jdk21-openjdk
COPY ./ProjectTest.war /usr/local/tomcat/webapps
