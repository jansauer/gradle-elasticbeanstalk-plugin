package de.jansauer.elasticbeanstalk

import org.gradle.api.Project
import org.gradle.api.provider.Property

class ElasticBeanstalkEnvironmentExtension {

  String name

  final Property<String> region
  final Property<String> application
  final Property<String> environment

  void setRegion(String region) {
    this.region.set(region)
  }

  void setApplication(String application) {
    this.application.set(application)
  }

  void setEnvironment(String environment) {
    this.environment.set(environment)
  }

  ElasticBeanstalkEnvironmentExtension(Project project) {
    this.name = name
    region = project.objects.property(String)
    application = project.objects.property(String)
    environment = project.objects.property(String)
  }
}
