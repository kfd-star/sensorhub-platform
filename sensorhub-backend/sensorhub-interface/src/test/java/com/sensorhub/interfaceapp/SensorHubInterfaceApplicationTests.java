package com.sensorhub.interfaceapp;

import com.sensorhub.clientsdk.client.SensorHubClient;
import com.sensorhub.clientsdk.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * Basic smoke tests for the interface app.
 */
@SpringBootTest
class SensorHubInterfaceApplicationTests {

    @Resource
    private SensorHubClient sensorHubClient;

    @Test
    void contextLoads() {
        String result = sensorHubClient.getNameByGet("sensorhub");
        User user = new User();
        user.setUsername("sensorhub-demo-user");
        String usernameByPost = sensorHubClient.getUsernameByPost(user);
        System.out.println(result);
        System.out.println(usernameByPost);
    }
}
