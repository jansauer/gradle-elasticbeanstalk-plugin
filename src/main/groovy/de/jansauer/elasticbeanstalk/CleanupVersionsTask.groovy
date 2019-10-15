package de.jansauer.elasticbeanstalk

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient
import software.amazon.awssdk.services.elasticbeanstalk.model.DeleteApplicationVersionRequest
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest

import java.util.regex.Pattern

class CleanupVersionsTask extends DefaultTask {

  @Input
  final Property<String> applicationName = project.objects.property(String)

  @Input
  final Property<Integer> versionToPreserve = project.objects.property(Integer)

  @Input
  final Property<Pattern> versionToRemoveRegex = project.objects.property(Pattern)

  CleanupVersionsTask() {
    setDescription('Cleanup unused ElasticBeanstalk Application Versions.')
    setGroup('aws')
  }

  @TaskAction
  def cleanupVersions() {
    logger.debug("Cleaning up elastic beanstalk versions for application '{}'", applicationName.get())
    def client = ElasticBeanstalkClient.builder()
        .region(Region.EU_CENTRAL_1)
        .build()

    client.describeApplicationVersions(DescribeApplicationVersionsRequest
        .builder()
            .applicationName(applicationName.get())
            .build())
        .applicationVersions()
        .findAll({ it.versionLabel() ==~ versionToRemoveRegex.get() })
        .drop(versionToPreserve.get())
        .each {
      client.deleteApplicationVersion(DeleteApplicationVersionRequest.builder()
          .applicationName(applicationName.get())
          .versionLabel(it.versionLabel())
          .deleteSourceBundle(true)
          .build())
      logger.info("Beanstalk application version with label '${it.versionLabel()}' was deleted.")
    }
  }
}
