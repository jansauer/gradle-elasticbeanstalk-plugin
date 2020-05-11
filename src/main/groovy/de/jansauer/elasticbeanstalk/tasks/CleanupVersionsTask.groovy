package de.jansauer.elasticbeanstalk.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient
import software.amazon.awssdk.services.elasticbeanstalk.model.DeleteApplicationVersionRequest
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest

class CleanupVersionsTask extends DefaultTask {

  @Input
  final Property<String> applicationRegion = project.objects.property(String)

  @Input
  final Property<String> applicationName = project.objects.property(String)

  @Input
  final Property<Integer> versionsToPreserve = project.objects.property(Integer)

  @TaskAction
  def cleanupVersions() {
    logger.debug("Cleaning up AWS Elastic Beanstalk versions for application '{}'", applicationName.get())
    def client = ElasticBeanstalkClient.builder()
      .region(Region.of(applicationRegion.get()))
      .build()

    def versions = client.describeApplicationVersions(DescribeApplicationVersionsRequest.builder()
      .applicationName(applicationName.get())
      .build())
      .applicationVersions()

    logger.info("Found {} AWS Elastic Beanstalk application versions", versions.size())
    logger.error("Found {} AWS Elastic Beanstalk application versions", versions.size())

    versions.drop(versionsToPreserve.get())
      .each {
        client.deleteApplicationVersion(DeleteApplicationVersionRequest.builder()
          .applicationName(applicationName.get())
          .versionLabel(it.versionLabel())
          .deleteSourceBundle(true)
          .build())
        logger.lifecycle("Deleted AWS Elastic Beanstalk application version '${it.versionLabel()}'.")
      }
  }
}
