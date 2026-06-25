# Use Tomcat 9 with JDK 17 as base image
FROM tomcat:9.0-jdk17

# Optional: clean default apps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the generated WAR file into the container webapps as ROOT.war
COPY target/meeting-room-booking-system.war /usr/local/tomcat/webapps/ROOT.war

# Expose port 8080
EXPOSE 8080

# Start Tomcat
CMD ["catalina.sh", "run"]
