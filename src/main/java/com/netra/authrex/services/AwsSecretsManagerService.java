package com.netra.authrex.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netra.commons.models.BaseEntity;
import com.netra.commons.util.BasicUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Map;

@Service
public class AwsSecretsManagerService {

    private  String SECRET_NAME;  // Replace with your actual secret name
    private Region REGION; // Change to your AWS region

    private final SecretsManagerClient secretsManagerClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.region}")
    private String region;

    public AwsSecretsManagerService() {
       // System.setProperty("aws.profile", "iac-cdk");
        SECRET_NAME = System.getenv("SECRET_NAME");
        String passedRegion = System.getenv("DEFAULT_REGION");
        REGION = BasicUtil.validString(region) ? Region.of(passedRegion) : Region.US_EAST_1;

        this.secretsManagerClient = SecretsManagerClient.builder()
                .region(REGION)
                .credentialsProvider(DefaultCredentialsProvider.create()) // Uses IAM role or profile
                .build();
    }

    public Map<String, Object> getSecret() {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(SECRET_NAME)
                .build();

        GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
        try {
            Map<String, Object> s = objectMapper.readValue(response.secretString(), Map.class);
            System.out.println("CRED: "+s);
            //{password=kLq3+'7^'$,YH'DR4LS<y2L+uR*tCq$`,
            // dbname=key_generator_db, engine=postgres,
            // port=5432,
            // dbInstanceIdentifier=rdspostgresstack-rdsinstance1d827d17-x4izbzfkjp4e,
            // host=rdspostgresstack-rdsinstance1d827d17-x4izbzfkjp4e.clk62cykcrfg.us-east-1.rds.amazonaws.com,
            // username=postgres}
            return s;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse secret", e);
        }
    }
}

