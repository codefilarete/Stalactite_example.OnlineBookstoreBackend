package com.switix.onlinebookstore.repository;

import com.switix.onlinebookstore.NoOpPasswordEncoder;
import com.switix.onlinebookstore.TestData;
import com.switix.onlinebookstore.model.*;
import org.codefilarete.reflection.AccessorDefinition;
import org.codefilarete.reflection.ValueAccessPoint;
import org.codefilarete.stalactite.dsl.MappingEase;
import org.codefilarete.stalactite.dsl.embeddable.FluentEmbeddableMappingBuilder;
import org.codefilarete.stalactite.dsl.entity.FluentEntityMappingBuilder;
import org.codefilarete.stalactite.dsl.naming.IndexNamingStrategy;
import org.codefilarete.stalactite.dsl.naming.JoinColumnNamingStrategy;
import org.codefilarete.stalactite.engine.EntityPersister;
import org.codefilarete.stalactite.engine.PersistenceContext;
import org.codefilarete.stalactite.spring.repository.config.EnableStalactiteRepositories;
import org.codefilarete.stalactite.sql.ddl.Size;
import org.codefilarete.stalactite.sql.ddl.structure.Column;
import org.codefilarete.tool.Strings;
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

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codefilarete.stalactite.dsl.MappingEase.entityBuilder;
import static org.codefilarete.stalactite.dsl.idpolicy.IdentifierPolicy.databaseAutoIncrement;
import static org.codefilarete.stalactite.dsl.naming.IndexNamingStrategy.SnakeCaseIndexNamingStrategy.DEFAULT_SUFFIX;
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
@EnableJpaRepositories(basePackageClasses = AppUserRepository.class,
        // we only want to scan for JpaRepository to avoid picking Stalactite repositories here, else we get bean conflicts
        includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = JpaRepository.class)
)
@ContextConfiguration(classes = {
        // required for TestData
        NoOpPasswordEncoder.class,
        AppUserRepository.class})
@DataJpaTest

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

@Import({TestData.class, AppUserRepositoryTest.TestDataSourceConfig.class})

@EnableStalactiteRepositories(includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = StalactiteAppUserRepository.class))
public class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private StalactiteAppUserRepository stalactiteAppUserRepository;

    @RepeatedTest(10)
    void findByEmail() {
        Chrono chrono = new Chrono();
        Optional<AppUser> allByCategoryIdAndIsRemoved_JPA = appUserRepository.findByEmail("user@example.com");
        assertThat(allByCategoryIdAndIsRemoved_JPA).isNotEmpty();
        long timeSpentByJPA = chrono.getElapsedTime();
        chrono.start();
        Optional<AppUser> allByCategoryIdAndIsRemoved_Stalactite = stalactiteAppUserRepository.findByEmail("user@example.com");
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
        public EntityPersister<AppUser, Long> appUserEntityPersister(PersistenceContext persistenceContext) {
            FluentEmbeddableMappingBuilder<Address> addressConfiguration = MappingEase.embeddableBuilder(Address.class)
                    .map(Address::getStreet).mandatory()
                    .map(Address::getZipCode).mandatory()
                    .mapOneToOne(Address::getCity, entityBuilder(City.class, Long.class)
                            .mapKey(City::getId, databaseAutoIncrement())
                            .map(City::getCityName).columnName("city_name").mandatory())
                        .withColumnNaming(accessorDefinition -> Strings.snakeCase(accessorDefinition.getName()))
                    .mapOneToOne(Address::getCountry, entityBuilder(Country.class, Long.class)
                            .mapKey(Country::getId, databaseAutoIncrement())
                            .map(Country::getCountryName).columnName("country_name").columnSize(Size.length(255)).mandatory())
                    .map(Address::getBuildingNumber).mandatory()
                    .map(Address::getApartmentNumber);

            FluentEntityMappingBuilder<BillingAddress, Long> billingAddressConfiguration = entityBuilder(BillingAddress.class, Long.class)
                    .mapKey(Address::getId, databaseAutoIncrement())
                    .map(BillingAddress::getPhoneNumber).mandatory()
                    .withColumnNaming(accessorDefinition -> Strings.snakeCase(accessorDefinition.getName()))
                    .onTable("billing_address")
                    .mapSuperClass(addressConfiguration)
                    .withJoinColumnNaming(new JoinColumnNamingStrategy() {
                        @Override
                        public String giveName(AccessorDefinition accessorDefinition, Column<?, ?> column) {
                            return Strings.snakeCase(accessorDefinition.getName())+ "_" + column.getName();
                        }
                    });

            FluentEntityMappingBuilder<ShippingAddress, Long> shippingAddressConfiguration = entityBuilder(ShippingAddress.class, Long.class)
                    .mapKey(Address::getId, databaseAutoIncrement())
                    .map(ShippingAddress::getName).mandatory()
                    .onTable("shipping_address")
                    .mapSuperClass(addressConfiguration)
                    .withJoinColumnNaming(new JoinColumnNamingStrategy() {
                        @Override
                        public String giveName(AccessorDefinition accessorDefinition, Column<?, ?> column) {
                            return Strings.snakeCase(accessorDefinition.getName())+ "_" + column.getName();
                        }
                    });

            FluentEntityMappingBuilder<ShoppingSession, Long> shoppingSessionConfiguration = entityBuilder(ShoppingSession.class, Long.class)
                    .mapKey(ShoppingSession::getId, databaseAutoIncrement())
                    .onTable("shopping_session")
                    .map(ShoppingSession::getTotal).mandatory();

            FluentEntityMappingBuilder<AppUser, Long> appUserEntityMappingBuilder = entityBuilder(AppUser.class, Long.class)
                    .onTable("app_user")
                    .mapKey(AppUser::getId, databaseAutoIncrement())
                    .map(AppUser::getName).mandatory()
                    .map(AppUser::getLastname).mandatory()
                    .map(AppUser::getPassword).mandatory()
                    .map(AppUser::getEmail).mandatory().unique()
                    .mapManyToOne(AppUser::getRole, entityBuilder(Role.class, Long.class)
                            .mapKey(Role::getId, databaseAutoIncrement())
                            .map(Role::getName).columnSize(Size.length(255)).unique().mandatory())
                        .columnName("role_id")
                    .mandatory()
                    .mapOneToOne(AppUser::getBillingAddress, billingAddressConfiguration).mappedBy(BillingAddress::getAppUser).unique()
                        .mappedBy("app_user_id")
                    .mapOneToMany(AppUser::getShippingAddresses, shippingAddressConfiguration).mappedBy(ShippingAddress::setAppUser)
                        .mappedBy("app_user_id")
                    .mapOneToOne(AppUser::getShoppingSession, shoppingSessionConfiguration).mappedBy(ShoppingSession::getAppUser).unique()//.mandatory()
                        .mappedBy("app_user_id")
                    .withIndexNaming(new IndexNamingStrategy() {
                        @Override
                        public String giveName(ValueAccessPoint<?> propertyAccessor, @Nullable String columnName) {
                            if (columnName != null) {
                                return columnName + "_" + DEFAULT_SUFFIX;
                            } else {
                                AccessorDefinition accessorDefinition = AccessorDefinition.giveDefinition(propertyAccessor);
                                if (accessorDefinition.getDeclaringClass().equals(BillingAddress.class) && accessorDefinition.getName().equals("appUser")) {
                                    // customizing index name for email unique constraint
                                    return "billing_address_app_user_id_key";
                                }
                                if (accessorDefinition.getDeclaringClass().equals(ShoppingSession.class) && accessorDefinition.getName().equals("appUser")) {
                                    // customizing index name for email unique constraint
                                    return "shopping_session_app_user_id_key";
                                }
                                // we create a unique name because most of the time (always), database index names have a schema scope,
                                // not a table one, thus their uniqueness must be on that scope too.
                                return Strings.snakeCase(
                                        accessorDefinition.getDeclaringClass().getSimpleName()
                                                + "_" + accessorDefinition.getName()
                                                + "_" + DEFAULT_SUFFIX);
                            }
                        }
                    })
                    ;
            return appUserEntityMappingBuilder.build(persistenceContext);
        }
    }
}
