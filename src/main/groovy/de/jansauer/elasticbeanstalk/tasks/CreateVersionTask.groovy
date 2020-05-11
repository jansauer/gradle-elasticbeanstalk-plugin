package de.jansauer.elasticbeanstalk.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateApplicationVersionRequest
import software.amazon.awssdk.services.elasticbeanstalk.model.S3Location
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import javax.annotation.Nullable
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CreateVersionTask extends DefaultTask {

  @Input
  final Property<String> applicationRegion = project.objects.property(String)

  @Input
  final Property<String> applicationName = project.objects.property(String)

  @Input
  final Property<String> versionBucket = project.objects.property(String)

  @Input
  final Property<String> versionLabel = project.objects.property(String)

  @Input
  final Property<String> versionDescription = project.objects.property(String)

  @TaskAction
  def execute() {
    def label = versionLabel.getOrElse(project.version)
    def key = "${applicationName.get()}-${label}.zip"

    def image = project.jib.to.image
    def dockerrun = """
{
  "AWSEBDockerrunVersion": "1",
  "Image": {
    "Name": "${image}",
    "Update": "true"
  },
  "Ports": [
    {
      "ContainerPort": "5001"
    }
  ]
}
    """

    /*
    Build zip artifact
     */

    def artifact = new ByteArrayOutputStream()
    ZipOutputStream zip = new ZipOutputStream(artifact);

    ZipEntry dockerrunEntry = new ZipEntry("Dockerrun.aws.json");
    zip.putNextEntry(dockerrunEntry);
    zip.write(dockerrun.bytes)

    // TODO add ebextensions

    zip.close();

    /*
    Upload zip artifact
     */

    def s3Client = S3Client.builder()
      .region(Region.of(applicationRegion.get()))
      .build()

    s3Client.putObject(
      PutObjectRequest.builder()
        .bucket(versionBucket.get())
        .key(key)
        .build(),
      RequestBody.fromBytes(artifact.toByteArray())
    )

    /*
    Create beanstalk version
     */

    def ebClient = ElasticBeanstalkClient.builder()
      .region(Region.of(applicationRegion.get()))
      .build()

    ebClient.createApplicationVersion(CreateApplicationVersionRequest.builder()
      .applicationName(applicationName.get())
      .versionLabel(label)
      .description(versionDescription.get())
      .sourceBundle(S3Location.builder()
        .s3Bucket(versionBucket.get())
        .s3Key(key)
        .build())
      .process(true)
      .build())

    logger.info("Created beanstalk application version for '{}' with label '{}'", applicationName.get(), label)
  }
}
