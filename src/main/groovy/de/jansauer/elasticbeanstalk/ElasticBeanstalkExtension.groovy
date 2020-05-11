package de.jansauer.elasticbeanstalk

import de.jansauer.elasticbeanstalk.tasks.CurrentVersionTask
import de.jansauer.elasticbeanstalk.tasks.DeployVersionTask
import org.gradle.api.Project
import org.gradle.api.provider.Property

class ElasticBeanstalkExtension {

  /**
   * Default version description
   */
  public static final String DEFAULT_VERSION_DESCRIPTION = ''

  /**
   * Default number of versions to preserve on cleanup
   */
  public static final int DEFAULT_VERSIONS_TO_PRESERVE = 30

  /**
   * AWS Region the version should be created in
   */
  final Property<String> region

  /**
   * Name of the AWS Elastic Beanstalk application the version should be created for
   */
  final Property<String> application

  /**
   * AWS S3 Bucket the version artifact should be stored in
   */
  final Property<String> bucket

  /**
   * Label used for the version
   */
  final Property<String> versionLabel

  /**
   * Description used for the version
   */
  final Property<String> versionDescription

  /**
   *
   */
  def environments = []

  /**
   * Number of versions to preserve on cleanup
   */
  final Property<Integer> versionsToPreserve

  private Project project

  ElasticBeanstalkExtension(Project project) {
    region = project.objects.property(String)
    application = project.objects.property(String)
    bucket = project.objects.property(String)
    versionLabel = project.objects.property(String)
    versionDescription = project.objects.property(String)
    versionDescription.set(DEFAULT_VERSION_DESCRIPTION)
    versionsToPreserve = project.objects.property(Integer)
    versionsToPreserve.set(DEFAULT_VERSIONS_TO_PRESERVE)

    // store project for registering environments later
    this.project = project
  }

  def environment(Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    def environment = new ElasticBeanstalkEnvironmentExtension(project)
    closure.delegate = environment
    closure()
    environments.add(environment)

    project.tasks.register("DeployChanges${environment.name}", CurrentVersionTask) {
      description = 'Collect all changes til the current version on the `${environment.name}` environment'
      group = 'aws'
      applicationRegion = environment.region.orElse(region)
      applicationName = environment.application.orElse(application)
      environmentName = environment.environment.orElse(environment.name)
    }

    project.tasks.register("Deploy${environment.name}", DeployVersionTask) {
      description = 'Deploy the current version on the `${environment.name}` environment'
      group = 'aws'
      applicationRegion = environment.region.orElse(region)
      applicationName = environment.application.orElse(application)
      environmentName = environment.environment.orElse(environment.name)
      environmentVersion = versionLabel
    }
  }
}
