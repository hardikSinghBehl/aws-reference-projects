package com.behl.encryptor.helper;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

@Slf4j
@Configuration
public class KMSKeyInitializer implements BeforeAllCallback {
	
	private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.2");
	
	private static final LocalStackContainer localStackContainer = new LocalStackContainer(LOCALSTACK_IMAGE)
			.withCopyFileToContainer(MountableFile.forClasspathResource("init-kms.sh", 0744), "/etc/localstack/init/ready.d/init-kms.sh")
			.withServices(Service.KMS)
			.waitingFor(Wait.forLogMessage(".*Executed init-kms.sh.*", 1));
	
	@Override
	public void beforeAll(final ExtensionContext context) {
		log.info("Creating localstack container : {}", LOCALSTACK_IMAGE);
		
		localStackContainer.start();
		addConfigurationProperties();
		
		log.info("Successfully started localstack container : {}", LOCALSTACK_IMAGE);
	}
	
	@Bean
	public KmsClient testKmsClient() {
		var accessKey = localStackContainer.getAccessKey();
		var secretAccessKey = localStackContainer.getSecretKey();
		var regionName = localStackContainer.getRegion();
		var endpointUri = localStackContainer.getEndpoint();

		var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretAccessKey));

		return KmsClient.builder()
				.region(Region.of(regionName))
				.endpointOverride(endpointUri)
				.credentialsProvider(credentials)
				.build();
	}

	private void addConfigurationProperties() {
		System.setProperty("aws.kms.key-id", "00000000-1111-2222-3333-000000000000");
	}

}