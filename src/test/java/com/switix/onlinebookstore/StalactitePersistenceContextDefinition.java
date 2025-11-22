package com.switix.onlinebookstore;

import javax.annotation.Nullable;
import java.sql.SQLException;

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
import org.codefilarete.jumper.schema.DefaultSchemaElementCollector;
import org.codefilarete.jumper.schema.PostgreSQLSchemaElementCollector;
import org.codefilarete.jumper.schema.difference.PostgreSQLSchemaDiffer;
import org.codefilarete.reflection.AccessorDefinition;
import org.codefilarete.reflection.ValueAccessPoint;
import org.codefilarete.stalactite.dsl.MappingEase;
import org.codefilarete.stalactite.dsl.embeddable.FluentEmbeddableMappingBuilder;
import org.codefilarete.stalactite.dsl.entity.FluentEntityMappingBuilder;
import org.codefilarete.stalactite.dsl.naming.AssociationTableNamingStrategy;
import org.codefilarete.stalactite.dsl.naming.IndexNamingStrategy;
import org.codefilarete.stalactite.dsl.naming.JoinColumnNamingStrategy;
import org.codefilarete.stalactite.engine.PersistenceContext;
import org.codefilarete.stalactite.engine.configurer.FluentEntityMappingConfigurationSupport;
import org.codefilarete.stalactite.sql.PostgreSQLDialectBuilder;
import org.codefilarete.stalactite.sql.ddl.DDLDeployer;
import org.codefilarete.stalactite.sql.ddl.Size;
import org.codefilarete.stalactite.sql.ddl.structure.Column;
import org.codefilarete.stalactite.sql.ddl.structure.PrimaryKey;
import org.codefilarete.stalactite.sql.ddl.structure.Table;
import org.codefilarete.tool.Strings;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import static org.codefilarete.stalactite.dsl.idpolicy.IdentifierPolicy.databaseAutoIncrement;
import static org.codefilarete.stalactite.dsl.naming.AssociationTableNamingStrategy.HIBERNATE;
import static org.codefilarete.stalactite.dsl.naming.IndexNamingStrategy.SnakeCaseIndexNamingStrategy.DEFAULT_SUFFIX;

public class StalactitePersistenceContextDefinition {
	
	@Test
	void persistenceContext() throws SQLException {
		FluentEmbeddableMappingBuilder<Address> addressConfiguration = addressConfiguration();
		
		FluentEntityMappingBuilder<BillingAddress, Long> billingAddressConfiguration = adaptedEntityBuilder(BillingAddress.class, Long.class)
				.mapKey(Address::getId, databaseAutoIncrement())
				.map(BillingAddress::getPhoneNumber).mandatory()
				.mapSuperClass(addressConfiguration);
		
		FluentEntityMappingBuilder<ShippingAddress, Long> shippingAddressConfiguration = adaptedEntityBuilder(ShippingAddress.class, Long.class)
				.mapKey(Address::getId, databaseAutoIncrement())
				.map(ShippingAddress::getName).mandatory()
				.mapSuperClass(addressConfiguration);
		
		FluentEntityMappingBuilder<ShoppingSession, Long> shoppingSessionConfiguration = adaptedEntityBuilder(ShoppingSession.class, Long.class)
				.mapKey(ShoppingSession::getId, databaseAutoIncrement())
				.map(ShoppingSession::getTotal).mandatory();
		FluentEntityMappingBuilder<AppUser, Long> userConfiguration = userConfiguration(billingAddressConfiguration, shippingAddressConfiguration, shoppingSessionConfiguration);
		
		FluentEntityMappingBuilder<Book, Long> bookConfiguration = bookConfiguration();
		
		FluentEntityMappingBuilder<OrderDetail, Long> orderDetailConfiguration = orderDetailConfiguration(
				billingAddressConfiguration,
				shippingAddressConfiguration,
				bookConfiguration,
				userConfiguration);
		
		FluentEntityMappingBuilder<CartItem, Long> cartItemConfiguration = cartItemConfiguration(shoppingSessionConfiguration, bookConfiguration);
		
		org.postgresql.ds.PGSimpleDataSource dataSource = new PGSimpleDataSource();
		dataSource.setUrl("jdbc:postgresql://localhost:5432/postgres");
		dataSource.setDatabaseName("postgres");
		dataSource.setCurrentSchema("bookstore_stit");
		dataSource.setUser("admin");
		dataSource.setPassword("admin");
		
		PersistenceContext persistenceContext = new PersistenceContext(dataSource, PostgreSQLDialectBuilder.defaultPostgreSQLDialect());
		
		billingAddressConfiguration.build(persistenceContext);
		shippingAddressConfiguration.build(persistenceContext);
		userConfiguration.build(persistenceContext);
		bookConfiguration.build(persistenceContext);
		orderDetailConfiguration.build(persistenceContext);
		cartItemConfiguration.build(persistenceContext);
		
		DDLDeployer ddlDeployer = new DDLDeployer(persistenceContext);
		ddlDeployer.deployDDL();
		persistenceContext.getConnectionProvider().giveConnection().commit();
	}
	
	@Test
	void difference() throws SQLException {
		org.postgresql.ds.PGSimpleDataSource dataSource1 = new PGSimpleDataSource();
		dataSource1.setUrl("jdbc:postgresql://localhost:5432/postgres");
		dataSource1.setCurrentSchema("bookstore_stit");
		dataSource1.setUser("admin");
		dataSource1.setPassword("admin");
		
		DefaultSchemaElementCollector schemaElementCollector1 = new PostgreSQLSchemaElementCollector(dataSource1.getConnection().getMetaData());
		schemaElementCollector1
				.withCatalog("postgres")
				.withSchema("bookstore_stit")
				.withTableNamePattern("%");
		
		org.postgresql.ds.PGSimpleDataSource dataSource2 = new PGSimpleDataSource();
		dataSource2.setUrl("jdbc:postgresql://localhost:5432/postgres");
		dataSource2.setCurrentSchema("Bookstore");
		dataSource2.setUser("admin");
		dataSource2.setPassword("admin");
		
		DefaultSchemaElementCollector schemaElementCollector2 = new PostgreSQLSchemaElementCollector(dataSource2.getConnection().getMetaData());
		schemaElementCollector2
				.withCatalog("postgres")
				.withSchema("bookstore")
				.withTableNamePattern("%");
		
		PostgreSQLSchemaDiffer schemaDiffer = new PostgreSQLSchemaDiffer();
		schemaDiffer.compareAndPrint(schemaElementCollector1.collect(), schemaElementCollector2.collect());
	}
	
	private FluentEmbeddableMappingBuilder<Address> addressConfiguration() {
		return MappingEase.embeddableBuilder(Address.class)
				.map(Address::getStreet).mandatory()
				.map(Address::getZipCode).mandatory()
				.mapOneToOne(Address::getCity, adaptedEntityBuilder(City.class, Long.class)
						.mapKey(City::getId, databaseAutoIncrement())
					.map(City::getCityName).columnName("city_name").mandatory())
				.mapOneToOne(Address::getCountry, adaptedEntityBuilder(Country.class, Long.class)
						.mapKey(Country::getId, databaseAutoIncrement())
					.map(Country::getCountryName).columnName("country_name").columnSize(Size.length(255)).mandatory())
				.map(Address::getBuildingNumber).mandatory()
				.map(Address::getApartmentNumber);
	}
	
	private FluentEntityMappingBuilder<AppUser, Long> userConfiguration(FluentEntityMappingBuilder<BillingAddress, Long> billingAddressConfiguration, FluentEntityMappingBuilder<ShippingAddress, Long> shippingAddressConfiguration, FluentEntityMappingBuilder<ShoppingSession, Long> shoppingSessionConfiguration) {
		return adaptedEntityBuilder(AppUser.class, Long.class)
				.onTable("app_user")
				.mapKey(AppUser::getId, databaseAutoIncrement())
				.map(AppUser::getName).mandatory()
				.map(AppUser::getLastname).mandatory()
				.map(AppUser::getPassword).mandatory()
				.map(AppUser::getEmail).mandatory().unique()
				.mapManyToOne(AppUser::getRole, adaptedEntityBuilder(Role.class, Long.class)
						.mapKey(Role::getId, databaseAutoIncrement())
					.map(Role::getName).columnSize(Size.length(255)).unique().mandatory())
				.mandatory()
				.mapOneToOne(AppUser::getBillingAddress, billingAddressConfiguration).mappedBy(BillingAddress::getAppUser).unique()
				.mapOneToMany(AppUser::getShippingAddresses, shippingAddressConfiguration).mappedBy(ShippingAddress::setAppUser)
				.mapOneToOne(AppUser::getShoppingSession, shoppingSessionConfiguration).mappedBy(ShoppingSession::getAppUser).unique().mandatory()
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
				});
	}
	
	private FluentEntityMappingBuilder<Book, Long> bookConfiguration() {
		return adaptedEntityBuilder(Book.class, Long.class)
				.mapKey(Book::getId, databaseAutoIncrement())
				.map(Book::getIsRemoved).mandatory().columnName("is_removed")
				.map(Book::getTitle).mandatory()
				.map(Book::getDescription).mandatory().columnSize(Size.length(2048))
				.map(Book::getPrice).mandatory().columnSize(Size.fixedPoint(10, 2))
				.map(Book::getImageUrl).columnName("image_url").mandatory()
				.map(Book::getPublicationYear).columnName("publication_year").mandatory()
				.map(Book::getIsbn).mandatory()
				.mapManyToOne(Book::getCategory, adaptedEntityBuilder(Category.class, Long.class)
						.mapKey(Category::getId, databaseAutoIncrement())
					.map(Category::getName).mandatory())
				.reverseCollection(Category::getCategoryBooks)
				.mapManyToOne(Book::getInventory, adaptedEntityBuilder(BookInventory.class, Long.class)
						.mapKey(BookInventory::getId, databaseAutoIncrement())
					.map(BookInventory::getQuantity).mandatory())
				.mandatory()
				.mapManyToMany(Book::getBookAuthors, adaptedEntityBuilder(Author.class, Long.class)
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
	}
	
	private FluentEntityMappingBuilder<OrderDetail, Long> orderDetailConfiguration(FluentEntityMappingBuilder<BillingAddress, Long> billingAddressConfiguration,
																				   FluentEntityMappingBuilder<ShippingAddress, Long> shippingAddressConfiguration,
																				   FluentEntityMappingBuilder<Book, Long> bookConfiguration,
																				   FluentEntityMappingBuilder<AppUser, Long> userConfiguration) {
		return adaptedEntityBuilder(OrderDetail.class, Long.class)
				.mapKey(OrderDetail::getId, databaseAutoIncrement())
				.map(OrderDetail::getTotal).mandatory()
				.map(OrderDetail::getCreatedAt)
				.mapManyToOne(OrderDetail::getAppUser, userConfiguration).mandatory()
				.mapOneToOne(OrderDetail::getBillingAddress, billingAddressConfiguration)
					.mandatory()
				.mapManyToOne(OrderDetail::getShippingAddress, shippingAddressConfiguration)
					.mandatory()
				.mapOneToMany(OrderDetail::getOrderItems, adaptedEntityBuilder(OrderItem.class, Long.class)
						.mapKey(OrderItem::getId, databaseAutoIncrement())
						.mapManyToOne(OrderItem::getBook, bookConfiguration).mandatory()
						.map(OrderItem::getQuantity).mandatory())
				.mappedBy(OrderItem::getOrderDetail).mandatory()
				.mapManyToOne(OrderDetail::getPayMethod, adaptedEntityBuilder(PayMethod.class, Long.class)
						.mapKey(PayMethod::getId, databaseAutoIncrement())
						.map(PayMethod::getName).mandatory()
						.map(PayMethod::getImageUrl).mandatory())
				.mandatory()
				.mapOneToOne(OrderDetail::getShipmentMethod, adaptedEntityBuilder(ShipmentMethod.class, Long.class)
						.mapKey(ShipmentMethod::getId, databaseAutoIncrement())
						.map(ShipmentMethod::getName).mandatory()
						.map(ShipmentMethod::getImageUrl).mandatory()
						.map(ShipmentMethod::getPrice).mandatory().columnSize(Size.fixedPoint(10, 2)))
				.mandatory()
				.mapManyToOne(OrderDetail::getOrderStatus, adaptedEntityBuilder(OrderStatus.class, Long.class)
						.mapKey(OrderStatus::getId, databaseAutoIncrement())
						.map(OrderStatus::getStatus).mandatory());
	}
	
	private FluentEntityMappingBuilder<CartItem, Long> cartItemConfiguration(FluentEntityMappingBuilder<ShoppingSession, Long> shoppingSessionConfiguration, FluentEntityMappingBuilder<Book, Long> bookConfiguration) {
		return adaptedEntityBuilder(CartItem.class, Long.class)
				.mapKey(CartItem::getId, databaseAutoIncrement())
				.map(CartItem::getQuantity).mandatory()
				.mapManyToOne(CartItem::getBook, bookConfiguration).mandatory()
				.mapManyToOne(CartItem::getShoppingSession, shoppingSessionConfiguration).mandatory();
	}
	
	/**
	 * Adapts official Stalactite entity build with local naming rules
	 *
	 * @param classToPersist
	 * @param identifierType
	 * @return
	 * @param <T>
	 * @param <I>
	 */
	static <T, I> FluentEntityMappingBuilder<T, I> adaptedEntityBuilder(Class<T> classToPersist, Class<I> identifierType) {
		return new FluentEntityMappingConfigurationSupport<T, I>(classToPersist)
				.withTableNaming(aClass -> Strings.snakeCase(aClass.getSimpleName()))
				.withAssociationTableNaming(HIBERNATE)
				.withColumnNaming(accessorDefinition -> Strings.snakeCase(accessorDefinition.getName()))
				.withJoinColumnNaming(new JoinColumnNamingStrategy() {
					@Override
					public String giveName(AccessorDefinition accessorDefinition, Column<?, ?> column) {
						return Strings.snakeCase(accessorDefinition.getName()) + "_" + column.getName();
					}
				});
	}
}
