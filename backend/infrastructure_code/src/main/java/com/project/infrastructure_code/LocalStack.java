package com.project.infrastructure_code;

import org.apache.commons.logging.Log;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedEc2Service;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalStack extends Stack {
    private final Vpc vpc;
    private final Cluster ecsCluster;

    public LocalStack(final App scope, final String id, final StackProps props){
        super(scope, id, props);

        this.vpc = createVpc();

        DatabaseInstance authServiceDb =
                createDatabase("AuthServiceDB", "auth-database");

        DatabaseInstance patientServiceDb =
                createDatabase("ProfileServiceDB", "user-database");

        DatabaseInstance contentServiceDb =
                createDatabase("ContentServiceDB" , "content-database");

        DatabaseInstance recommendationServiceDb =
                createDatabase("RecommendationServiceDB" , "recommendation-db");

        CfnHealthCheck authDbHealthCheck =
                createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");

        CfnHealthCheck patientDbHealthCheck =
                createDbHealthCheck(patientServiceDb, "ProfileServiceDBHealthCheck");

        CfnHealthCheck contentServiceHealthCheck =
                createDbHealthCheck(contentServiceDb , "ContentDBSeriveHealthCheck");

        CfnHealthCheck recommendationServiceHealthCheck =
                createDbHealthCheck(recommendationServiceDb , "RecommendationDBServiceHeathCheck");

        CfnCluster mskCLuster = createCluster();
        this.ecsCluster = createEcsCluster();

        FargateService authService =
                createFargateService(
                        "AuthService",
                        "magician05/authenication-service:latest",
                        List.of(8080),
                        authServiceDb,
                        Map.of(
                                "SECURITY_JWT_SECRET",
                                "AVx9KQp7mT3ZrE2Wn8YFJHcL5B0uD4aS6GkPqR1oI",
                                "SPRING_REDIS_HOST", "auth-redis",
                                "SPRING_REDIS_PORT", "6379"
                        )
                );

        authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(authServiceDb);

        FargateService userService =
                createFargateService(
                        "UserService",
                        "magician05/user-service:latest",
                        List.of(8080),
                        patientServiceDb,
                        Map.of(
                                "SPRING_REDIS_HOST", "user-redis",
                                "SPRING_REDIS_PORT", "6379"
                        )
                );

        userService.getNode().addDependency(patientDbHealthCheck);
        userService.getNode().addDependency(patientServiceDb);

        FargateService contentService =
                createFargateService(
                        "ContentService",
                        "magician05/content-service:latest",
                        List.of(8080),
                        contentServiceDb,
                        Map.of(
                                "SPRING_REDIS_HOST", "content-redis",
                                "SPRING_REDIS_PORT", "6379"
                        )
                );

        contentService.getNode().addDependency(contentServiceHealthCheck);
        contentService.getNode().addDependency(contentServiceDb);

        FargateService recommendationService =
                createFargateService(
                        "RecommendationService",
                        "magician05/recommendation-service:latest",
                        List.of(8080),
                        recommendationServiceDb,
                        Map.of()
                );

        recommendationService.getNode().addDependency(recommendationServiceHealthCheck);
        recommendationService.getNode().addDependency(recommendationServiceDb);

        FargateService notificationService =
                createFargateService(
                        "NotificationService",
                        "magician05/notification-service:latest",
                        List.of(8080),
                        null,
                        Map.of(
                                "SPRING_MAIL_USERNAME", "xxxxxxx@gmail.com",
                                "SPRING_MAIL_PASSWORD", "xxx xxxx xxx xxx xxx"
                        )
                );

        FargateService searchService =
                createFargateService(
                        "SearchService",
                        "magician05/search-service:latest",
                        List.of(8080),
                        null,
                        Map.of(
                                "SPRING_ELASTICSEARCH_URIS", "http://localhost:9200"
                        )
                );
        createApiGatewayService(List.of(11111));

    }

    private Vpc createVpc(){
        return Vpc.Builder
                .create(this, "AnimeVerseVPC")
                .vpcName("AnimeVerseVPC")
                .maxAzs(2)
                .build();
    }

    private DatabaseInstance createDatabase(String id, String dbName){
        return DatabaseInstance.Builder
                .create(this, id)
                .engine(DatabaseInstanceEngine.postgres(
                        PostgresInstanceEngineProps.builder()
                                .version(PostgresEngineVersion.VER_17_2)
                                .build()))
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("admin_user"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id){
        return CfnHealthCheck.Builder.create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                        .ipAddress(db.getDbInstanceEndpointAddress())
                        .requestInterval(30)
                        .failureThreshold(3)
                        .build())
                .build();
    }

    public CfnCluster createCluster() {
        return CfnCluster
                .Builder
                .create(this , "MskCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster
                        .BrokerNodeGroupInfoProperty
                        .builder()
                        .instanceType("kafka.m5.xlarge")
                        .clientSubnets(vpc
                                .getPrivateSubnets()
                                .stream()
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList())
                        )
                        .brokerAzDistribution("DEFAULT")
                        .build()
                )
                .build();
    }

    private Cluster createEcsCluster() {
        return Cluster
                .Builder
                .create(this , "AnimeVerseCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions
                        .builder()
                        .name("anime-verse.local")
                        .build()
                )
                .build();
    }

    private FargateService createFargateService(
            String id,
            String imageName,
            List<Integer> ports,
            DatabaseInstance databaseInstance,
            Map<String , String> env
    ) {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition
                .Builder
                .create(this , id+"Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions.Builder containerDefinitionOptions = ContainerDefinitionOptions
                .builder()
                .image(ContainerImage.fromRegistry(imageName))
                .portMappings(ports
                        .stream()
                        .map(port -> PortMapping
                                .builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build()
                        )
                        .toList()
                )
                .logging(LogDriver
                        .awsLogs(AwsLogDriverProps
                                .builder()
                                .logGroup(LogGroup
                                        .Builder
                                        .create(this , id + "LogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build()
                                )
                                .streamPrefix(imageName)
                                .build()
                        )
                );

        Map<String , String> envVariable = new HashMap<>();
        envVariable.put("SPRING_KAFKA_BOOTSTRAP_SERVERS" , "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

        if(env != null){
            envVariable.putAll(env);
        }

        if(databaseInstance != null){
            envVariable.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s"
                    .formatted(databaseInstance.getDbInstanceEndpointAddress(),
                            databaseInstance.getDbInstanceEndpointPort(),
                            imageName)
            );
            envVariable.put("SPRING_DATASOURCE_USERNAME" , "admin_user");
            envVariable.put("SPRING_DATASOURCE_PASSWORD" , databaseInstance.getSecret().secretValueFromJson("password").toString());
            envVariable.put("SPRING_JPA_HIBERNATE_DDL_AUTO" , "update");
            envVariable.put("SPRING_SQL_INIT_MODE" , "always");
            envVariable.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        containerDefinitionOptions.environment(envVariable);
        taskDefinition.addContainer(imageName + "Container" , containerDefinitionOptions.build());

        return FargateService
                .Builder
                .create(this , id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .serviceName(imageName)
                .build();
    }

    private void createApiGatewayService(List<Integer> ports) {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition
                .Builder
                .create(this, "ApiGatewayTask")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions containerDefinitionOptions = ContainerDefinitionOptions
                .builder()
                .image(ContainerImage.fromRegistry("api-gateway"))
                .environment(
                        Map.of("SPRING_PROFILES_ACTIVE" , "prod")
                )
                .portMappings(ports
                        .stream()
                        .map(port -> PortMapping
                                .builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build()
                        )
                        .toList()
                )
                .logging(LogDriver
                        .awsLogs(AwsLogDriverProps
                                .builder()
                                .logGroup(LogGroup
                                        .Builder
                                        .create(this, "ApiGatewayLogGroup")
                                        .logGroupName("/ecs/api-gateway")
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build()
                                )
                                .streamPrefix("api-gateway")
                                .build()
                        )
                )
                .build();

        taskDefinition.addContainer("ApiGatewayContainer" , containerDefinitionOptions);

        ApplicationLoadBalancedFargateService fargateService =
                ApplicationLoadBalancedFargateService
                        .Builder
                        .create(this , "api-gateway-service")
                        .cluster(ecsCluster)
                        .serviceName("api-gateway")
                        .taskDefinition(taskDefinition)
                        .desiredCount(1)
                        .healthCheckGracePeriod(Duration.minutes(1))
                        .build();
    }

    public static void main(String[] args) {
        App app = new App(AppProps.builder().outdir("./cdk.out").build());
        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        new LocalStack(app, "localstack", props);
        app.synth();
        System.out.println("App synthesizing in progress...");
    }
}
