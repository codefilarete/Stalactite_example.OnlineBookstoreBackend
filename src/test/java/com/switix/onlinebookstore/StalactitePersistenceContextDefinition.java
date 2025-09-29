package com.switix.onlinebookstore;

import java.sql.SQLException;

import com.switix.onlinebookstore.model.Address;
import com.switix.onlinebookstore.model.AppUser;
import com.switix.onlinebookstore.model.Author;
import com.switix.onlinebookstore.model.BillingAddress;
import com.switix.onlinebookstore.model.Book;
import com.switix.onlinebookstore.model.BookInventory;
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
import org.codefilarete.stalactite.engine.FluentEntityMappingBuilder;
import org.codefilarete.stalactite.engine.MappingEase;
import org.codefilarete.stalactite.engine.PersistenceContext;
import org.codefilarete.stalactite.sql.PostgreSQLDialectBuilder;
import org.codefilarete.stalactite.sql.ddl.DDLDeployer;
import org.codefilarete.stalactite.sql.ddl.Size;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import static org.codefilarete.stalactite.engine.ColumnOptions.IdentifierPolicy.databaseAutoIncrement;

public class StalactitePersistenceContextDefinition {
	
	@Test
	void persistenceContext() throws SQLException {
		FluentEntityMappingBuilder<Address, Long> addressConfiguration = addressConfiguration();
		
		FluentEntityMappingBuilder<BillingAddress, Long> billingAddressConfiguration = MappingEase.entityBuilder(BillingAddress.class, Long.class)
//				.mapKey(BillingAddress::getId, databaseAutoIncrement())
				.map(BillingAddress::getPhoneNumber)
				.mapSuperClass(addressConfiguration);
		
		FluentEntityMappingBuilder<ShippingAddress, Long> shippingAddressConfiguration = MappingEase.entityBuilder(ShippingAddress.class, Long.class)
//				.mapKey(ShippingAddress::getId, databaseAutoIncrement())
				.map(ShippingAddress::getName)
				.mapSuperClass(addressConfiguration);
		
		FluentEntityMappingBuilder<AppUser, Long> userConfiguration = userConfiguration(billingAddressConfiguration, shippingAddressConfiguration);
		
		FluentEntityMappingBuilder<Book, Long> bookConfiguration = bookConfiguration();
		
		FluentEntityMappingBuilder<OrderDetail, Long> orderDetailConfiguration = orderDetailConfiguration(billingAddressConfiguration, shippingAddressConfiguration);
		
		
		org.postgresql.ds.PGSimpleDataSource dataSource = new PGSimpleDataSource();
//		dataSource.setUrl("jdbc:postgresql://localhost:5432/");
		dataSource.setUrl("jdbc:postgresql://localhost:5432/Bookstore_stit");
		dataSource.setDatabaseName("Bookstore_stit");
		dataSource.setUser("postgres");
		dataSource.setPassword("postgres");
		
		PersistenceContext persistenceContext = new PersistenceContext(dataSource, PostgreSQLDialectBuilder.defaultPostgreSQLDialect());
		
		addressConfiguration.build(persistenceContext);
		billingAddressConfiguration.build(persistenceContext);
		shippingAddressConfiguration.build(persistenceContext);
		userConfiguration.build(persistenceContext);
		bookConfiguration.build(persistenceContext);
		orderDetailConfiguration.build(persistenceContext);
		
		DDLDeployer ddlDeployer = new DDLDeployer(persistenceContext);
		ddlDeployer.deployDDL();
		persistenceContext.getConnectionProvider().giveConnection().commit();
	}
	
	@Test
	void difference() throws SQLException {
		org.postgresql.ds.PGSimpleDataSource dataSource1 = new PGSimpleDataSource();
		dataSource1.setUrl("jdbc:postgresql://localhost:5432/Bookstore_stit");
		dataSource1.setUser("postgres");
		dataSource1.setPassword("postgres");
		
		DefaultSchemaElementCollector schemaElementCollector1 = new PostgreSQLSchemaElementCollector(dataSource1.getConnection().getMetaData());
		schemaElementCollector1
				.withCatalog(null)
				.withSchema("public")
				.withTableNamePattern("%");
		
		org.postgresql.ds.PGSimpleDataSource dataSource2 = new PGSimpleDataSource();
		dataSource2.setUrl("jdbc:postgresql://localhost:5432/Bookstore");
		dataSource2.setUser("postgres");
		dataSource2.setPassword("postgres");
		
		DefaultSchemaElementCollector schemaElementCollector2 = new PostgreSQLSchemaElementCollector(dataSource2.getConnection().getMetaData());
		schemaElementCollector2
				.withCatalog(null)
				.withSchema("public")
				.withTableNamePattern("%");
		
		PostgreSQLSchemaDiffer schemaDiffer = new PostgreSQLSchemaDiffer();
		schemaDiffer.compareAndPrint(schemaElementCollector1.collect(), schemaElementCollector2.collect());
	}
	
	private FluentEntityMappingBuilder<Address, Long> addressConfiguration() {
		return MappingEase.entityBuilder(Address.class, Long.class)
				.mapKey(Address::getId, databaseAutoIncrement())
				.map(Address::getStreet)
				.map(Address::getZipCode)
				.mapOneToOne(Address::getCity, MappingEase.entityBuilder(City.class, Long.class)
						.mapKey(City::getId, databaseAutoIncrement())
						.map(City::getCityName))
				.mapOneToOne(Address::getCountry, MappingEase.entityBuilder(Country.class, Long.class)
						.mapKey(Country::getId, databaseAutoIncrement())
						.map(Country::getCountryName))
				.map(Address::getBuildingNumber)
				.map(Address::getApartmentNumber);
	}
	
	private FluentEntityMappingBuilder<AppUser, Long> userConfiguration(FluentEntityMappingBuilder<BillingAddress, Long> billingAddressConfiguration, FluentEntityMappingBuilder<ShippingAddress, Long> shippingAddressConfiguration) {
		return MappingEase.entityBuilder(AppUser.class, Long.class)
				.onTable("app_user")
				.mapKey(AppUser::getId, databaseAutoIncrement())
				.map(AppUser::getName).mandatory()
				.map(AppUser::getLastname).mandatory()
				.map(AppUser::getPassword).mandatory()
				.map(AppUser::getEmail).mandatory()
				.mapManyToOne(AppUser::getRole, MappingEase.entityBuilder(Role.class, Long.class)
						.mapKey(Role::getId, databaseAutoIncrement())
						.map(Role::getName))
				.mapOneToOne(AppUser::getBillingAddress, billingAddressConfiguration)
				.mapOneToMany(AppUser::getShippingAddresses, shippingAddressConfiguration).reverselySetBy(ShippingAddress::setAppUser)
				.mapOneToOne(AppUser::getShoppingSession, MappingEase.entityBuilder(ShoppingSession.class, Long.class)
						.mapKey(ShoppingSession::getId, databaseAutoIncrement())
						.map(ShoppingSession::getTotal)).mappedBy(ShoppingSession::getAppUser);
	}
	
	private FluentEntityMappingBuilder<Book, Long> bookConfiguration() {
		return MappingEase.entityBuilder(Book.class, Long.class)
				.mapKey(Book::getId, databaseAutoIncrement())
				.map(Book::getIsRemoved)
				.map(Book::getTitle)
				.map(Book::getDescription)
				.map(Book::getPrice)
				.map(Book::getImageUrl)
				.map(Book::getPublicationYear)
				.map(Book::getIsbn)
				.mapManyToOne(Book::getCategory, MappingEase.entityBuilder(Category.class, Long.class)
						.mapKey(Category::getId, databaseAutoIncrement())
						.map(Category::getName)).reverseCollection(Category::getCategoryBooks)
				.mapManyToOne(Book::getInventory, MappingEase.entityBuilder(BookInventory.class, Long.class)
						.mapKey(BookInventory::getId, databaseAutoIncrement())
						.map(BookInventory::getQuantity))
				.mapManyToMany(Book::getBookAuthors, MappingEase.entityBuilder(Author.class, Long.class)
						.mapKey(Author::getId, databaseAutoIncrement())
						.map(Author::getName)).reverseCollection(Author::getAuthorBooks);
	}
	
	private FluentEntityMappingBuilder<OrderDetail, Long> orderDetailConfiguration(FluentEntityMappingBuilder<BillingAddress, Long> billingAddressConfiguration, FluentEntityMappingBuilder<ShippingAddress, Long> shippingAddressConfiguration) {
		return MappingEase.entityBuilder(OrderDetail.class, Long.class)
				.mapKey(OrderDetail::getId, databaseAutoIncrement())
				.map(OrderDetail::getTotal)
				.map(OrderDetail::getCreatedAt)
				.mapOneToOne(OrderDetail::getBillingAddress, billingAddressConfiguration)
				.mapManyToOne(OrderDetail::getShippingAddress, shippingAddressConfiguration)
				.mapOneToMany(OrderDetail::getOrderItems, MappingEase.entityBuilder(OrderItem.class, Long.class)
						.mapKey(OrderItem::getId, databaseAutoIncrement())
						.map(OrderItem::getQuantity))
				.mapManyToOne(OrderDetail::getPayMethod, MappingEase.entityBuilder(PayMethod.class, Long.class)
						.mapKey(PayMethod::getId, databaseAutoIncrement())
						.map(PayMethod::getName)
						.map(PayMethod::getImageUrl))
				.mapOneToOne(OrderDetail::getShipmentMethod, MappingEase.entityBuilder(ShipmentMethod.class, Long.class)
						.mapKey(ShipmentMethod::getId, databaseAutoIncrement())
						.map(ShipmentMethod::getName)
						.map(ShipmentMethod::getImageUrl)
						.map(ShipmentMethod::getPrice))
				.mapManyToOne(OrderDetail::getOrderStatus, MappingEase.entityBuilder(OrderStatus.class, Long.class)
						.mapKey(OrderStatus::getId, databaseAutoIncrement())
						.map(OrderStatus::getStatus).mandatory());
	}
	
}
