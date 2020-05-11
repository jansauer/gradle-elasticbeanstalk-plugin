package de.jansauer.elasticbeanstalk.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeEnvironmentsRequest
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeEventsRequest
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentStatus
import software.amazon.awssdk.services.elasticbeanstalk.model.UpdateEnvironmentRequest

class DeployVersionTask extends DefaultTask {

  @Input
  final Property<String> applicationRegion = project.objects.property(String)

  @Input
  final Property<String> applicationName = project.objects.property(String)

  @Input
  final Property<String> environmentName = project.objects.property(String)

  @Input
  final Property<String> environmentVersion = project.objects.property(String)

  @TaskAction
  def deployVersion() {
    def client = ElasticBeanstalkClient.builder()
      .region(Region.of(applicationRegion.get()))
      .build()

    // Environment needs to be ready to start an update
    // We query the environment in a loop to wait and report it status is ready
    // Unfortunately the elastic beanstalk sdk doesn't have waiters yet
    // https://aws.amazon.com/blogs/developer/waiters-in-the-aws-sdk-for-java/
    for (def retries = 0; retries <= 30; retries++) {
      if (retries == 30) { // max tries exceeded
        throw new GradleException('Environment ready timed-out.')
      }

      def result = client.describeEnvironments(DescribeEnvironmentsRequest.builder()
        .applicationName(applicationName.get())
        .environmentNames(environmentName.get())
        .build())
        .environments.first()

      if (result.status == 'Ready' && result.versionLabel == environmentVersion.get()) {
        throw new GradleException('The version to deploy and currently used are the same.')
      } else if (result.status == 'Ready') {
        break
      } else {
        logger.lifecycle("Status is '{}'. Waiting for environment to get ready to update.", result.status)
        sleep(30 * 1000) // 30sec
      }
    }

    client.updateEnvironment(UpdateEnvironmentRequest.builder()
      .applicationName(applicationName.get())
      .environmentName(environmentName.get())
      .versionLabel(environmentVersion.get())
      .build())
      .with {
        logger.lifecycle("Updating environment to version '{}' ...", environmentVersion.get())
      }

    // The call 'updateEnvironment' only starts the update and returns immediately
    // We query the environment in a loop to wait and report on the update progress
    def paginationToken = ''
    for (def retries = 0; retries <= 60; retries++) {
      if (retries == 60) { // max tries exceeded
        throw new GradleException('Environment Update timed-out.')
      }

      sleep(30 * 1000) // 30sec

      def events = client.describeEvents(DescribeEventsRequest.builder()
        .applicationName(applicationName.get())
        .environmentName(environmentName.get())
        .nextToken(paginationToken)
        .build())

      paginationToken = events.nextToken()

      events.events()
        .reverse()
        .each {
          logger.lifecycle("{} [{}] {}", it.eventDate(), it.severity(), it.message())
        }

      def result = client.describeEnvironments(DescribeEnvironmentsRequest.builder()
        .applicationName(applicationName.get())
        .environmentNames(environmentName.get())
        .build())
        .environments.first()

      if (result.versionLabel() == environmentVersion.get() && result.status() == EnvironmentStatus.READY) {
        logger.lifecycle("Version reported is '{}'. Environment is '{}' and '{}'.", result.versionLabel(), result.status(), result.health())
        break
      } else {
        logger.info("Version reported is '{}'. Environment is '{}' and '{}'.", result.versionLabel(), result.status(), result.health())
      }
    }
  }
}
