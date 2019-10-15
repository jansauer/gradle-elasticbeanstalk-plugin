package de.jansauer.elasticbeanstalk

import org.gradle.internal.impldep.org.junit.Rule
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateApplicationRequest
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateApplicationVersionRequest
import software.amazon.awssdk.services.elasticbeanstalk.model.DeleteApplicationRequest
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest
import software.amazon.awssdk.services.elasticbeanstalk.model.S3Location
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import spock.lang.Shared
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class CleanupVersionsTaskIntegrationTest extends Specification {

  @Shared
  String applicationName

  @Shared
  S3Client s3Client

  @Shared
  String s3BucketName

  @Shared
  ElasticBeanstalkClient ebClient

  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder()

  File buildFile

  def setupSpec() {
    applicationName = 'Gradle Plugin Test'
    s3BucketName = 'elasticbeanstalk-eu-central-1-640667059075'
    s3Client = S3Client.builder()
        .region(Region.EU_CENTRAL_1)
        .build()
    ebClient = ElasticBeanstalkClient.builder()
        .region(Region.EU_CENTRAL_1)
        .build()
    ebClient.createApplication(CreateApplicationRequest.builder()
        .applicationName(applicationName)
        .build())
    createApplicationVersion('0.0.0')
    createApplicationVersion('1.0.0-2-g4b03a14')
    createApplicationVersion('1.0.2-20-g4b03a14')
    createApplicationVersion('1.5.20-21-g4b03a14')
    createApplicationVersion('20.10.1-5-g4b03a14')
    createApplicationVersion('1.0.0')
  }

  def setup() {
    testProjectDir.create()
    buildFile = testProjectDir.newFile('build.gradle')
  }

  def "should cleanup old application versions"() {
    given:
    buildFile << """
        plugins {
          id 'de.jansauer.elasticbeanstalk'
        }
        
        elasticBeanstalk {
          applicationName = 'Gradle Plugin Test'
          versionToPreserve = 2
        }
    """

    when:
    def result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.root)
        .withArguments('cleanupVersions')
        .withDebug(true)
        .withPluginClasspath()
        .build()
    print result.output

    then:
    result.task(':cleanupVersions').outcome == SUCCESS

    when:
    def remainingVersions = ebClient.describeApplicationVersions(DescribeApplicationVersionsRequest.builder()
        .applicationName(applicationName)
        .build())
        .applicationVersions()

    then:
    remainingVersions.size() == 4
    remainingVersions.collectNested({
      it.versionLabel
    }) == ['1.0.0', '20.10.1-5-g4b03a14', '1.5.20-21-g4b03a14', '0.0.0']

    where:
    gradleVersion << ['4.10', '4.10.1', '4.10.2', '5.0', '4.10.3', '5.1', '5.1.1']
  }

  def cleanupSpec() {
    ebClient.deleteApplication(DeleteApplicationRequest.builder()
        .applicationName(applicationName)
        .build())
  }

  def createApplicationVersion(String version) {
    s3Client.putObject(PutObjectRequest.builder()
        .bucket(s3BucketName)
        .key(version + '.jar')
        .build(),
        RequestBody.fromString(version as String))
    ebClient.createApplicationVersion(CreateApplicationVersionRequest.builder()
        .applicationName(applicationName)
        .versionLabel(version)
        .description('Test application version ' + version)
        .sourceBundle(S3Location.builder()
            .s3Bucket(s3BucketName)
            .s3Key(version + '.jar')
            .build())
        .process(false)
        .build())
  }
}
