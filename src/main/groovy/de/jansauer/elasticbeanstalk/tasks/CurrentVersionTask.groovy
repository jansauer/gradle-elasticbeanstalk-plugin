package de.jansauer.elasticbeanstalk.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeEnvironmentsRequest

class CurrentVersionTask extends DefaultTask {

  @Input
  final Property<String> applicationRegion = project.objects.property(String)

  @Input
  final Property<String> applicationName = project.objects.property(String)

  @Input
  final Property<String> environmentName = project.objects.property(String)


//  @Output
//  final Property<String> versionLabel = project.objects.property(String)


  @TaskAction
  def execute() {

    println applicationRegion.get()
    println applicationName.get()
    println environmentName.get()

    def client = ElasticBeanstalkClient.builder()
      .region(Region.of(applicationRegion.get()))
      .build()

    def environmentsResult = client.describeEnvironments(DescribeEnvironmentsRequest.builder()
      .applicationName(applicationName.get())
      .environmentNames(environmentName.get())
      .build())
      .environments

    if (environmentsResult.size() != 1) {
      logger.error("Unable to find `{}` environment of `{}` in `{}`", environmentName.get(), applicationName.get(), applicationRegion.get())
      throw new GradleException('Unable to find environment')
    }

    def environmentResult = environmentsResult.first()

    logger.info("Environment `{}` is currently running version `{}`",
      environmentName.get(),
      environmentResult.versionLabel()
    )


    println project.grgit.isAncestorOf('dbe6e98139b988f7d573563df4248edb15bd9da8', '7d73ea0e95d5acf6f840796a549517173c245e76')

    project.grgit.log(includes: ['5e4c44b'], excludes: ['7d73ea0e95d5acf6f840796a549517173c245e76']).each { commit ->
      println "${commit.id} ${commit.shortMessage}"
      println commit.fullMessage
    }
  }
}
