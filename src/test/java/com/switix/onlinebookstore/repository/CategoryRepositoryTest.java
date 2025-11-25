package com.switix.onlinebookstore.repository;

import javax.sql.DataSource;
import java.util.List;

import com.switix.onlinebookstore.NoOpPasswordEncoder;
import com.switix.onlinebookstore.TestData;
import com.switix.onlinebookstore.dto.CategoryBookCountDto;
import com.switix.onlinebookstore.model.Address;
import com.switix.onlinebookstore.model.AppUser;
import com.switix.onlinebookstore.model.Author;
import com.switix.onlinebookstore.model.BillingAddress;
import com.switix.onlinebookstore.model.Book;
import com.switix.onlinebookstore.model.BookInventory;
import com.switix.onlinebookstore.model.CartItem;
import com.switix.onlinebookstore.model.Category;
import com.switix.onlinebookstore.model.City;
import com.switix.onlinebookstore.model.Country;
import com.switix.onlinebookstore.model.OrderDetail;
import com.switix.onlinebookstore.model.OrderItem;
import com.switix.onlinebookstore.model.OrderStatus;
import com.switix.onlinebookstore.model.PayMethod;
import com.switix.onlinebookstore.model.Role;
import com.switix.onlinebookstore.model.ShipmentMethod;
import com.switix.onlinebookstore.model.ShippingAddress;
import com.switix.onlinebookstore.model.ShoppingSession;
import org.codefilarete.reflection.AccessorDefinition;
import org.codefilarete.stalactite.dsl.naming.AssociationTableNamingStrategy;
import org.codefilarete.stalactite.engine.EntityPersister;
import org.codefilarete.stalactite.engine.ExecutableQuery;
import org.codefilarete.stalactite.engine.PersistenceContext;
import org.codefilarete.stalactite.query.model.OrderByChain;
import org.codefilarete.stalactite.query.model.QueryEase;
import org.codefilarete.stalactite.spring.repository.config.EnableStalactiteRepositories;
import org.codefilarete.stalactite.spring.repository.query.BeanQuery;
import org.codefilarete.stalactite.sql.ddl.Size;
import org.codefilarete.stalactite.sql.ddl.structure.Column;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.codefilarete.stalactite.dsl.MappingEase.entityBuilder;
import static org.codefilarete.stalactite.dsl.idpolicy.IdentifierPolicy.databaseAutoIncrement;
import static org.codefilarete.stalactite.query.model.Operators.count;
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
@EnableJpaRepositories(basePackageClasses = CategoryRepository.class,
        // we only want to scan for JpaRepository to avoid picking Stalactite repositories here, else we get bean conflicts
        includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = JpaRepository.class)
)
@ContextConfiguration(classes = {
        // required for TestData
        NoOpPasswordEncoder.class,
        CategoryRepository.class})
@DataJpaTest

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

@Import({TestData.class, CategoryRepositoryTest.TestDataSourceConfig.class})

@EnableStalactiteRepositories(includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = StalactiteCategoryRepository.class))
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StalactiteCategoryRepository stalactiteCategoryRepository;

    @RepeatedTest(10)
    void findAllByNameLikeIgnoreCase() {
        Chrono chrono = new Chrono();
        List<Category> allByCategoryIdAndIsRemoved_JPA = categoryRepository.findAllByNameLikeIgnoreCase("Fantasy");
        assertThat(allByCategoryIdAndIsRemoved_JPA).isNotEmpty();
        long timeSpentByJPA = chrono.getElapsedTime();
        chrono.start();
        List<Category> allByCategoryIdAndIsRemoved_Stalactite = stalactiteCategoryRepository.findAllByNameLikeIgnoreCase("Fantasy");
        assertThat(allByCategoryIdAndIsRemoved_Stalactite).isNotEmpty();
        long timeSpentByStalactite = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA)
                .usingRecursiveComparison()
                .ignoringFields("categoryBooks.price", "categoryBooks.category", "categoryBooks.bookAuthors", "categoryBooks.inventory")
                .isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentComparingJPAvsStalactiteResults = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA)
                .usingRecursiveComparison()
                        .ignoringFields("categoryBooks.price", "categoryBooks.category", "categoryBooks.bookAuthors", "categoryBooks.inventory")
                .isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);

        long timeSpentInPureComparison = chrono.getElapsedTime();
        long timeToFullyLoadJPAEntities = timeSpentByJPA + (timeSpentComparingJPAvsStalactiteResults - timeSpentInPureComparison);
        System.out.println("Time spent by JPA to fully load entities: " + timeToFullyLoadJPAEntities + " ms");
        System.out.println("Time spent by Stalactite to fully load entities: " + timeSpentByStalactite + " ms");
    }

    @RepeatedTest(10)
    void countBooksByCategory() {
        Chrono chrono = new Chrono();
        List<CategoryBookCountDto> allByCategoryIdAndIsRemoved_JPA = categoryRepository.countBooksByCategory();
        assertThat(allByCategoryIdAndIsRemoved_JPA).isNotEmpty();
        long timeSpentByJPA = chrono.getElapsedTime();
        chrono.start();
        List<CategoryBookCountDto> allByCategoryIdAndIsRemoved_Stalactite = stalactiteCategoryRepository.countBooksByCategory();
        assertThat(allByCategoryIdAndIsRemoved_Stalactite).isNotEmpty();
        long timeSpentByStalactite = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA)
                .usingRecursiveComparison()
                .isEqualTo(allByCategoryIdAndIsRemoved_Stalactite);
        long timeSpentComparingJPAvsStalactiteResults = chrono.getElapsedTime();
        chrono.start();
        assertThat(allByCategoryIdAndIsRemoved_JPA)
                .usingRecursiveComparison()
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

        @BeanQuery
        public ExecutableQuery<CategoryBookCountDto> countBooksByCategory(PersistenceContext persistenceContext) {
            Table<?> categoryTable = new Table<>("Category");
            Column<?, Long> categoryIdColumn = categoryTable.addColumn("id", Long.class);
            Column<?, String> categoryNameColumn = categoryTable.addColumn("name", String.class);
            Table<?> bookTable = new Table<>("Book");
            Column<?, Long> bookIdColumn = bookTable.addColumn("id", Long.class);
            Column<?, Long> categoryBookIdColumn = bookTable.addColumn("category_Id", Long.class);

            return persistenceContext.newQuery(QueryEase.
                    select(categoryIdColumn, categoryNameColumn, count(bookIdColumn))
                        .from(categoryTable).innerJoin(categoryIdColumn, categoryBookIdColumn)
                        .groupBy(categoryIdColumn, categoryNameColumn)
                        .orderBy(categoryNameColumn, OrderByChain.Order.ASC), CategoryBookCountDto.class)
                    .mapKey(CategoryBookCountDto::new, categoryIdColumn, categoryNameColumn, count(bookIdColumn));
        }

        @Bean
        public EntityPersister<Category, Long> categoryEntityPersister(PersistenceContext persistenceContext) {
            return entityBuilder(Category.class, Long.class)
                    .mapKey(Category::getId, databaseAutoIncrement())
                    .map(Category::getName).mandatory()
                    .mapOneToMany(Category::getCategoryBooks, entityBuilder(Book.class, Long.class)
                            .mapKey(Book::getId, databaseAutoIncrement())
                            .map(Book::getIsRemoved).mandatory().columnName("is_removed")
                            .map(Book::getTitle).mandatory()
                            .map(Book::getDescription).mandatory().columnSize(Size.length(2048))
                            .map(Book::getPrice).mandatory().columnSize(Size.fixedPoint(10, 2))
                            .map(Book::getImageUrl).columnName("image_url").mandatory()
                            .map(Book::getPublicationYear).columnName("publication_year").mandatory()
                            .map(Book::getIsbn).mandatory()
                            .mapManyToMany(Book::getBookAuthors, entityBuilder(Author.class, Long.class)
                                    .mapKey(Author::getId, databaseAutoIncrement())
                                    .map(Author::getName).mandatory())
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
                            })
                    )
                    .mappedBy(Book::getCategory)
                    .mappedBy("category_id")
                    .build(persistenceContext);
        }
    }
}