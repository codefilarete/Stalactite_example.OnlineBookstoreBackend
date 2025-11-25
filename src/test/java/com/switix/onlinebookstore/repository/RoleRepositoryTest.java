package com.switix.onlinebookstore.repository;

import com.switix.onlinebookstore.NoOpPasswordEncoder;
import com.switix.onlinebookstore.TestData;
import com.switix.onlinebookstore.model.*;
import org.codefilarete.stalactite.engine.EntityPersister;
import org.codefilarete.stalactite.engine.PersistenceContext;
import org.codefilarete.stalactite.spring.repository.config.EnableStalactiteRepositories;
import org.codefilarete.tool.trace.Chrono;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codefilarete.stalactite.dsl.MappingEase.entityBuilder;
import static org.codefilarete.stalactite.dsl.idpolicy.IdentifierPolicy.databaseAutoIncrement;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

@EntityScan(basePackageClasses = {
        Address.class,
        AppUser.class,
        Author.class,
        BillingAddress.class,
        Book.class,
        BookInventory.class,
        CartItem.class,
        Category.class,
        City.class,
        Country.class,
        OrderDetail.class,
        OrderItem.class,
        OrderStatus.class,
        PayMethod.class,
        Role.class,
        ShipmentMethod.class,
        ShippingAddress.class,
        ShoppingSession.class
})
@EnableJpaRepositories(basePackageClasses = RoleRepository.class,
        // we only want to scan for JpaRepository to avoid picking Stalactite repositories here, else we get bean conflicts
        includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = JpaRepository.class)
)
@ContextConfiguration(classes = {
        // required for TestData
        NoOpPasswordEncoder.class,
        RoleRepository.class})
@DataJpaTest

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

@Import({TestData.class, RoleRepositoryTest.TestDataSourceConfig.class})

@EnableStalactiteRepositories(includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = StalactiteRoleRepository.class))
public class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StalactiteRoleRepository stalactiteRoleRepository;

    @RepeatedTest(10)
    void findByName() {
        Chrono chrono = new Chrono();
        Optional<Role> allByCategoryIdAndIsRemoved_JPA = roleRepository.findByName("ROLE_CUSTOMER");
        assertThat(allByCategoryIdAndIsRemoved_JPA).isNotEmpty();
        long timeSpentByJPA = chrono.getElapsedTime();
        chrono.start();
        Optional<Role> allByCategoryIdAndIsRemoved_Stalactite = stalactiteRoleRepository.findByName("ROLE_CUSTOMER");
        assertThat(allByCategoryIdAndIsRemoved_Stalactite).isNotEmpty();
        long timeSpentByStalactite = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA)
                .usingRecursiveComparison()
                .ignoringFields("value.shippingAddresses")  // empty vs null
                .isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentComparingJPAvsStalactiteResults = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA)
                .usingRecursiveComparison()
                .ignoringFields("value.shippingAddresses")  // empty vs null
                .isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentInPureComparison = chrono.getElapsedTime();
        long timeToFullyLoadJPAEntities = timeSpentByJPA + (timeSpentComparingJPAvsStalactiteResults - timeSpentInPureComparison);
        System.out.println("Time spent by JPA to fully load entities: " + timeToFullyLoadJPAEntities + " ms");
        System.out.println("Time spent by Stalactite to fully load entities: " + timeSpentByStalactite + " ms");
    }

    @TestConfiguration
    public static class TestDataSourceConfig {
		
		@Bean
		public PostgreSQLContainer<?> database() {
			PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:14.11");
			postgreSQLContainer.start();
			return postgreSQLContainer;
		}
		
		@Bean
		@Primary
		public DataSource dataSource(JdbcDatabaseContainer<?> database) {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setUrl(database.getJdbcUrl());
			dataSource.setUsername(database.getUsername());
			dataSource.setPassword(database.getPassword());
			return dataSource;
		}

        @Bean
        public PersistenceContext persistenceContext(DataSource dataSource) {
            return new PersistenceContext(dataSource);
        }

        @Bean
        public EntityPersister<Role, Long> appUserEntityPersister(PersistenceContext persistenceContext) {
            return entityBuilder(Role.class, Long.class)
                    .mapKey(Role::getId, databaseAutoIncrement())
                    .map(Role::getName)
                    .build(persistenceContext);
        }
    }
}
