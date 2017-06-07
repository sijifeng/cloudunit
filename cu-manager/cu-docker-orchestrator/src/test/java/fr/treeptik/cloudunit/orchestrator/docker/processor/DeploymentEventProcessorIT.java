package fr.treeptik.cloudunit.orchestrator.docker.processor;

import static fr.treeptik.cloudunit.orchestrator.docker.test.TestCaseConstants.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import fr.treeptik.cloudunit.domain.resource.DeploymentEvent;
import fr.treeptik.cloudunit.domain.resource.DeploymentResource;
import fr.treeptik.cloudunit.domain.resource.ServiceResource;
import fr.treeptik.cloudunit.orchestrator.docker.config.CloudUnitConfiguration;
import fr.treeptik.cloudunit.orchestrator.docker.test.ContainerTemplate;
import fr.treeptik.cloudunit.orchestrator.docker.test.HttpTemplate;
import fr.treeptik.cloudunit.orchestrator.resource.ContainerResource;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@SpringBootTest
@AutoConfigureMockMvc
public class DeploymentEventProcessorIT {

    public static final String HELLOWORLD_ARTIFACT_URL
            = "https://github.com/Treeptik/cloudunit/releases/download/1.0/helloworld.war";

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private DeploymentEventProcessor deploymentEventProcessor;

    @Autowired
    private ContainerTemplate containerTemplate;

    @Autowired
    private HttpTemplate httpTemplate;

    @Autowired
    private CloudUnitConfiguration cloudUnitConfiguration;

    @Test
    public void deployHelloWorld() throws Exception {
        ContainerResource container = containerTemplate.createAndAssumeContainer(CONTAINER_NAME, IMAGE_NAME);
        try {
            containerTemplate.startContainer(container);
            DeploymentResource deploymentResource = new DeploymentResource("xxx", HELLOWORLD_ARTIFACT_URL);
            DeploymentEvent deploymentEvent = new DeploymentEvent();
            deploymentEvent.setType(DeploymentEvent.Type.DEPLOYED);
            deploymentEvent.setDeployment(deploymentResource);
            deploymentEvent.setService(ServiceResource.of().containerName(CONTAINER_NAME).build());
            deploymentEventProcessor.onDeploymentEvent(deploymentEvent);

            String domainName = cloudUnitConfiguration.getDomainName();
            String url = String.format("http://%s.%s/%s", CONTAINER_NAME, domainName, "xxx");
            String content = httpTemplate.getContent(url);
            assertThat(content, containsString("CloudUnit PaaS"));

        } finally {
            containerTemplate.waitWhilePending(container);
            containerTemplate.deleteContainerAndWait(container);
        }
    }
}
