package com.bmuschko.todo.webservice.repository;

import com.bmuschko.todo.webservice.model.ToDoItem;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ContextConfiguration(initializers = { DockerComposeIntegrationTest.Initializer.class })
public class DockerComposeIntegrationTest {

    private final static File PROJECT_DIR = new File(System.getProperty("project.dir"));
    private final static String POSTGRES_SERVICE_NAME = "database_1";
    private final static int POSTGRES_SERVICE_PORT = 5432;

    @Autowired
    private ToDoRepository repository;

    @ClassRule
    public static DockerComposeContainer environment = createComposeContainer();

    private static DockerComposeContainer createComposeContainer() {
        return new DockerComposeContainer(new File(PROJECT_DIR, "src/test/resources/compose-test.yml"))
                .withExposedService(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + getPostgresServiceUrl(),
                    "spring.datasource.username=postgres",
                    "spring.datasource.password=postgres",
                    "spring.datasource.driver-class-name=org.postgresql.Driver",
                    "spring.jpa.generate-ddl=true"
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    private static String getPostgresServiceUrl() {
        String postgresHost = environment.getServiceHost(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);
        Integer postgresPort = environment.getServicePort(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);
        String postgresHostAndPort = postgresHost + ":" + postgresPort;
        return "jdbc:postgresql://" + postgresHostAndPort + "/todo";
    }

    @Test
    public void testCanSaveNewToDoItem() {
        ToDoItem toDoItem = createToDoItem("Buy milk");
        assertNull(toDoItem.getId());
        repository.save(toDoItem);
        assertNotNull(toDoItem.getId());
    }

    private ToDoItem createToDoItem(String name) {
        ToDoItem toDoItem = new ToDoItem();
        toDoItem.setName(name);
        return toDoItem;
    }
}