package com.switix.onlinebookstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switix.onlinebookstore.NoOpPasswordEncoder;
import com.switix.onlinebookstore.TestData;
import com.switix.onlinebookstore.dto.OrderDetailCreationDto;
import com.switix.onlinebookstore.dto.OrderDetailDto;
import com.switix.onlinebookstore.dto.OrderItemDto;
import com.switix.onlinebookstore.dto.UpdateOrderDto;
import com.switix.onlinebookstore.exception.BookInsufficientStockException;
import com.switix.onlinebookstore.exception.EmptyShoppingCartException;
import com.switix.onlinebookstore.exception.OrderDetailNotFoundException;
import com.switix.onlinebookstore.model.*;
import com.switix.onlinebookstore.repository.OrderDetailRepository;
import com.switix.onlinebookstore.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
@EnableJpaRepositories(basePackageClasses = OrderDetailRepository.class)

// this annotation may look a duplication with @WebMvcTest, but it's required due to @WebMvcTest doesn't take PasswordEncoder into Account
@ContextConfiguration(classes = {
        TestData.class,
        // required for TestData
        NoOpPasswordEncoder.class,
        // adding classes required for controller
        OrderController.class
})

// we can't use @DataJpaTest because its @BootstrapWith conflicts with @WebMvcTest: Spring doesn't allow to above both of them in same test
@Transactional
@AutoConfigureDataJpa
@AutoConfigureTestEntityManager

@WebMvcTest(OrderController.class)
// we don't want to be bothered by security during controller tests (@WebMvcTest(excludeAutoConfigurations=..) doesn't work, don't know why)
@ImportAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void getOrderDetail_shouldReturnOk() throws Exception {
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setId(1L);

        when(orderService.getOrderDetail(1L)).thenReturn(orderDetail);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getOrderDetail_shouldReturnNotFound() throws Exception {
        when(orderService.getOrderDetail(anyLong()))
                .thenThrow(new OrderDetailNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderDetails_shouldReturnOk() throws Exception {

        when(orderService.getOrdersDetail(1L))
                .thenReturn(Collections.singletonList(new OrderDetailDto()));
        AppUser user = createMockUser();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);

        mockMvc.perform(get("/api/orders")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getOrderDetailsAdmin_shouldReturnOk() throws Exception {
        when(orderService.getOrdersDetailAdmin())
                .thenReturn(Arrays.asList(new OrderDetailDto(), new OrderDetailDto()));

        mockMvc.perform(get("/api/orders/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getOrderItems_shouldReturnOk() throws Exception {
        when(orderService.getOrderItems(1L))
                .thenReturn(Collections.singletonList(new OrderItemDto()));

        mockMvc.perform(get("/api/orders/1/orderItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createOrderDetail_shouldReturnCreated() throws Exception {
        AppUser user = createMockUser();
        OrderDetailCreationDto creationDto = new OrderDetailCreationDto();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setId(1L);

        when(orderService.createOrderDetail(any(OrderDetailCreationDto.class), anyLong()))
                .thenReturn(orderDetail);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);

        mockMvc.perform(post("/api/orders")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creationDto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void createOrderDetail_shouldReturnNoContentWhenCartEmpty() throws Exception {
        AppUser user = createMockUser();
        OrderDetailCreationDto creationDto = new OrderDetailCreationDto();

        when(orderService.createOrderDetail(any(OrderDetailCreationDto.class), anyLong()))
                .thenThrow(new EmptyShoppingCartException("Cart is empty"));
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);

        mockMvc.perform(post("/api/orders")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creationDto)))
                .andExpect(status().isNoContent());
    }

    @Test
    void createOrderDetail_shouldReturnConflictWhenInsufficientStock() throws Exception {
        AppUser user = createMockUser();
        OrderDetailCreationDto creationDto = new OrderDetailCreationDto();

        when(orderService.createOrderDetail(any(OrderDetailCreationDto.class), anyLong()))
                .thenThrow(new BookInsufficientStockException("Insufficient stock"));
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);

        mockMvc.perform(post("/api/orders")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creationDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateOrder_shouldReturnNoContent() throws Exception {
        UpdateOrderDto updateDto = new UpdateOrderDto();

        mockMvc.perform(patch("/api/orders/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateOrder_shouldReturnNotFoundWhenOrderNotExists() throws Exception {
        UpdateOrderDto updateDto = new UpdateOrderDto();

        doThrow(new OrderDetailNotFoundException("Order not found"))
                .when(orderService).updateOrder(any(UpdateOrderDto.class));

        mockMvc.perform(patch("/api/orders/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOrder_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/orders/admin/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteOrder_shouldReturnNotFoundWhenOrderNotExists() throws Exception {
        doThrow(new OrderDetailNotFoundException("Order not found"))
                .when(orderService).deleteOrder(anyLong());

        mockMvc.perform(delete("/api/orders/admin/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayMethods_shouldReturnOk() throws Exception {
//        when(payMethodRepository.findAll())
//                .thenReturn(Arrays.asList(new PayMethod(), new PayMethod()));

        mockMvc.perform(get("/api/orders/payMethods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getShipmentMethods_shouldReturnOk() throws Exception {
//        when(shipmentMethodRepository.findAll())
//                .thenReturn(Arrays.asList(new ShipmentMethod(), new ShipmentMethod()));

        mockMvc.perform(get("/api/orders/shipmentMethods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private AppUser createMockUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        Role role = new Role();
        role.setName("ROLE_ADMIN");
        user.setRole(role);
        ShoppingSession session = new ShoppingSession();
        session.setId(1L);
        user.setShoppingSession(session);
        return user;
    }

}