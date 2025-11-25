package com.switix.onlinebookstore.repository;

import com.switix.onlinebookstore.NoOpPasswordEncoder;
import com.switix.onlinebookstore.TestData;
import com.switix.onlinebookstore.model.*;
import org.codefilarete.reflection.AccessorDefinition;
import org.codefilarete.stalactite.dsl.entity.FluentEntityMappingBuilder;
import org.codefilarete.stalactite.dsl.naming.AssociationTableNamingStrategy;
import org.codefilarete.stalactite.engine.EntityPersister;
import org.codefilarete.stalactite.engine.PersistenceContext;
import org.codefilarete.stalactite.spring.repository.config.EnableStalactiteRepositories;
import org.codefilarete.stalactite.sql.ddl.Size;
import org.codefilarete.stalactite.sql.ddl.structure.PrimaryKey;
import org.codefilarete.stalactite.sql.ddl.structure.Table;
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

import javax.sql.DataSource;
import java.util.List;

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
@EnableJpaRepositories(basePackageClasses = BookRepository.class,
        // we only want to scan for JpaRepository to avoid picking Stalactite repositories here, else we get bean conflicts
        includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = JpaRepository.class)
)
@ContextConfiguration(classes = {
        // required for TestData
        NoOpPasswordEncoder.class,
        BookRepository.class})
@DataJpaTest

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

@Import({TestData.class, BookRepositoryTest.TestDataSourceConfig.class})

@EnableStalactiteRepositories(includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = StalactiteBookRepository.class))
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private StalactiteBookRepository stalactiteBookRepository;

    @RepeatedTest(10)
    void findAllByIsRemoved() {
        Chrono chrono = new Chrono();
        List<Book> allByCategoryIdAndIsRemoved_JPA = bookRepository.findAllByIsRemoved(false);
        assertThat(allByCategoryIdAndIsRemoved_JPA).isNotEmpty();
        long timeSpentByJPA = chrono.getElapsedTime();
        chrono.start();
        List<Book> allByCategoryIdAndIsRemoved_Stalactite = stalactiteBookRepository.findAllByIsRemoved(false);
        assertThat(allByCategoryIdAndIsRemoved_Stalactite).isNotEmpty();
        long timeSpentByStalactite = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA).usingRecursiveComparison().isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentComparingJPAvsStalactiteResults = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA).usingRecursiveComparison().isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentInPureComparison = chrono.getElapsedTime();
        long timeToFullyLoadJPAEntities = timeSpentByJPA + (timeSpentComparingJPAvsStalactiteResults - timeSpentInPureComparison);
        System.out.println("Time spent by JPA to fully load entities: " + timeToFullyLoadJPAEntities + " ms");
        System.out.println("Time spent by Stalactite to fully load entities: " + timeSpentByStalactite + " ms");
    }

    @RepeatedTest(10)
    void findAllByCategory_IdAndIsRemoved() {
        Chrono chrono = new Chrono();
        List<Book> allByCategoryIdAndIsRemoved_JPA = bookRepository.findAllByCategory_IdAndIsRemoved(1L, false);
        assertThat(allByCategoryIdAndIsRemoved_JPA).isNotEmpty();
        long timeSpentByJPA = chrono.getElapsedTime();
        chrono.start();
        List<Book> allByCategoryIdAndIsRemoved_Stalactite = stalactiteBookRepository.findAllByCategory_IdAndIsRemoved(1L, false);
        assertThat(allByCategoryIdAndIsRemoved_Stalactite).isNotEmpty();
        long timeSpentByStalactite = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA).usingRecursiveComparison().isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentComparingJPAvsStalactiteResults = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA).usingRecursiveComparison().isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentInPureComparison = chrono.getElapsedTime();
        long timeToFullyLoadJPAEntities = timeSpentByJPA + (timeSpentComparingJPAvsStalactiteResults - timeSpentInPureComparison);
        System.out.println("Time spent by JPA to fully load entities: " + timeToFullyLoadJPAEntities + " ms");
        System.out.println("Time spent by Stalactite to fully load entities: " + timeSpentByStalactite + " ms");
    }

    @RepeatedTest(10)
    void findAllByBookAuthors_IdAndIsRemoved() {
        Chrono chrono = new Chrono();
        List<Book> allByCategoryIdAndIsRemoved_JPA = bookRepository.findAllByBookAuthors_IdAndIsRemoved(1L, false);
        assertThat(allByCategoryIdAndIsRemoved_JPA).isNotEmpty();
        long timeSpentByJPA = chrono.getElapsedTime();
        chrono.start();
        List<Book> allByCategoryIdAndIsRemoved_Stalactite = stalactiteBookRepository.findAllByBookAuthors_IdAndIsRemoved(1L, false);
        assertThat(allByCategoryIdAndIsRemoved_Stalactite).isNotEmpty();
        long timeSpentByStalactite = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA)
                .usingRecursiveComparison()
                // category book are not mapped in Stalactite so we exclude them from comparison (see comment on mapping)
                .ignoringFields("category.categoryBooks")
                .isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentComparingJPAvsStalactiteResults = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA)
                .usingRecursiveComparison()
                // category book are not mapped in Stalactite so we exclude them from comparison (see comment on mapping)
                .ignoringFields("category.categoryBooks")
                .isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentInPureComparison = chrono.getElapsedTime();
        long timeToFullyLoadJPAEntities = timeSpentByJPA + (timeSpentComparingJPAvsStalactiteResults - timeSpentInPureComparison);
        System.out.println("Time spent by JPA to fully load entities: " + timeToFullyLoadJPAEntities + " ms");
        System.out.println("Time spent by Stalactite to fully load entities: " + timeSpentByStalactite + " ms");
    }

    @RepeatedTest(10)
    void findAllByTitleIsLikeIgnoreCaseAndIsRemoved() {
        Chrono chrono = new Chrono();
        List<Book> allByCategoryIdAndIsRemoved_JPA = bookRepository.findAllByTitleIsLikeIgnoreCaseAndIsRemoved("Harry Potter%", false);
        assertThat(allByCategoryIdAndIsRemoved_JPA).isNotEmpty();
        long timeSpentByJPA = chrono.getElapsedTime();
        chrono.start();
        List<Book> allByCategoryIdAndIsRemoved_Stalactite = stalactiteBookRepository.findAllByTitleIsLikeIgnoreCaseAndIsRemoved("Harry Potter%", false);
        assertThat(allByCategoryIdAndIsRemoved_Stalactite).isNotEmpty();
        long timeSpentByStalactite = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA)
                .usingRecursiveComparison()
                // category book are not mapped in Stalactite so we exclude them from comparison (see comment on mapping)
                .ignoringFields("category.categoryBooks")
                .isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentComparingJPAvsStalactiteResults = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA)
                .usingRecursiveComparison()
                // category book are not mapped in Stalactite so we exclude them from comparison (see comment on mapping)
                .ignoringFields("category.categoryBooks")
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
        public EntityPersister<Book, Long> bookEntityPersister(PersistenceContext persistenceContext) {
            FluentEntityMappingBuilder<Book, Long> bookEntityMappingBuilder = entityBuilder(Book.class, Long.class)
                    .mapKey(Book::getId, databaseAutoIncrement())
                    .map(Book::getIsRemoved).mandatory().columnName("is_removed")
                    .map(Book::getTitle).mandatory()
                    .map(Book::getDescription).mandatory().columnSize(Size.length(2048))
                    .map(Book::getPrice).mandatory().columnSize(Size.fixedPoint(10, 2))
                    .map(Book::getImageUrl).columnName("image_url").mandatory()
                    .map(Book::getPublicationYear).columnName("publication_year").mandatory()
                    .map(Book::getIsbn).mandatory()
                    .mapManyToOne(Book::getCategory, entityBuilder(Category.class, Long.class)
                            .mapKey(Category::getId, databaseAutoIncrement())
                            .map(Category::getName).mandatory()).columnName("category_id")
                    // we don't need the reverse collection to be filled, actually it's very dangerous when looking for specific Books
                    // because the query filters by Book Id which then also filter the category books due to the joins
                    //.reverseCollection(Category::getCategoryBooks)
                    .mapManyToOne(Book::getInventory, entityBuilder(BookInventory.class, Long.class)
                            .mapKey(BookInventory::getId, databaseAutoIncrement())
                            .map(BookInventory::getQuantity).mandatory()
                            .onTable("book_inventory")).columnName("inventory_id")
                       .mandatory()
                    .mapManyToMany(Book::getBookAuthors, entityBuilder(Author.class, Long.class)
                            .mapKey(Author::getId, databaseAutoIncrement())
                            .map(Author::getName).mandatory())
                      .reverseCollection(Author::getAuthorBooks)
                    .withAssociationTableNaming(new AssociationTableNamingStrategy.HibernateAssociationTableNamingStrategy() {

                        @Override
                        public <LEFTTABLE extends Table<LEFTTABLE>, RIGHTTABLE extends Table<RIGHTTABLE>, LEFTID, RIGHTID> ReferencedColumnNames<LEFTTABLE, RIGHTTABLE> giveColumnNames(
                                AccessorDefinition accessor,
                                PrimaryKey<LEFTTABLE, LEFTID> leftPrimaryKey,
                                PrimaryKey<RIGHTTABLE, RIGHTID> rightPrimaryKey) {
                            if (accessor.getName().equals("bookAuthors")) {
                                // customizing column names for bookAuthors association table
                                ReferencedColumnNames<LEFTTABLE, RIGHTTABLE> result = new ReferencedColumnNames<>();

                                // columns pointing to left table get same names as original ones
                                leftPrimaryKey.getColumns().forEach(column -> {
                                    String leftColumnName = Strings.uncapitalize(leftPrimaryKey.getTable().getName()) + "_" + column.getName();
                                    result.setLeftColumnName(column, leftColumnName);
                                });
                                // columns pointing to right table get a name that contains accessor definition name
                                rightPrimaryKey.getColumns().forEach(column -> {
                                    result.setRightColumnName(column, "author_id");
                                });
                                return result;
                            }
                            return super.giveColumnNames(accessor, leftPrimaryKey, rightPrimaryKey);
                        }
                    });
            return bookEntityMappingBuilder.build(persistenceContext);
        }
    }
}