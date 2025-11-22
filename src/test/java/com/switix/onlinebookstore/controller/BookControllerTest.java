package com.switix.onlinebookstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switix.onlinebookstore.NoOpPasswordEncoder;
import com.switix.onlinebookstore.TestData;
import com.switix.onlinebookstore.dto.SaveBookDto;
import com.switix.onlinebookstore.dto.UpdateBookDto;
import com.switix.onlinebookstore.exception.BookNotFoundException;
import com.switix.onlinebookstore.exception.CategoryNotFoundException;
import com.switix.onlinebookstore.model.*;
import com.switix.onlinebookstore.repository.OrderDetailRepository;
import com.switix.onlinebookstore.service.AppUserDetailsService;
import com.switix.onlinebookstore.service.BookService;
import com.switix.onlinebookstore.service.BookServiceImpl;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
        // adding classes required by controller
        BookController.class,
        BookServiceImpl.class,
        AppUserDetailsService.class})

// we can't use @DataJpaTest because its @BootstrapWith conflicts with @WebMvcTest: Spring doesn't allow to above both of them in same test
@Transactional
@AutoConfigureDataJpa
@AutoConfigureTestEntityManager

@WebMvcTest(value = BookController.class)
// we don't want to be bothered by security during controller tests (@WebMvcTest(excludeAutoConfigurations=..) doesn't work, don't know why)
@ImportAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    @Test
    void getBooks_shouldReturnListOfBooks() throws Exception {
        List<Book> books = Arrays.asList(new Book(), new Book());
        when(bookService.getAllBooks(false)).thenReturn(books);

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(bookService).getAllBooks(false);
    }

    @Test
    void getBook_whenBookExists_shouldReturnBook() throws Exception {
        Book book = new Book();
        book.setId(1L);
        when(bookService.getBook(1L)).thenReturn(Optional.of(book));

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(bookService).getBook(1L);
    }

    @Test
    void getBook_whenBookNotExists_shouldReturn404() throws Exception {
        when(bookService.getBook(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isNotFound());

        verify(bookService).getBook(1L);
    }

    @Test
    void getBooksByCategory_shouldReturnListOfBooks() throws Exception {
        List<Book> books = Arrays.asList(new Book(), new Book());
        when(bookService.getAllBooksByCategory(1L, false)).thenReturn(books);

        mockMvc.perform(get("/api/books/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(bookService).getAllBooksByCategory(1L, false);
    }

    @Test
    void getBooksMadeByAuthor_shouldReturnListOfBooks() throws Exception {
        List<Book> books = Arrays.asList(new Book());
        when(bookService.getAllBooksByAuthor(1L, false)).thenReturn(books);

        mockMvc.perform(get("/api/books/authors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(bookService).getAllBooksByAuthor(1L, false);
    }

    @Test
    void saveBook_shouldReturnCreatedWithLocation() throws Exception {
        SaveBookDto saveBookDto = new SaveBookDto();
        Book savedBook = new Book();
        savedBook.setId(1L);
        when(bookService.saveBook(any(SaveBookDto.class))).thenReturn(savedBook);

        mockMvc.perform(post("/api/books/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveBookDto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "http://localhost/api/books/1"));

        verify(bookService).saveBook(any(SaveBookDto.class));
    }

    @Test
//    @WithMockUser(roles = "ADMIN")
    void updateBook_shouldReturnNoContent() throws Exception {
        UpdateBookDto updateBookDto = new UpdateBookDto();
        doNothing().when(bookService).updateBook(any(UpdateBookDto.class));

        mockMvc.perform(patch("/api/books/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBookDto)))
                .andExpect(status().isNoContent());

        verify(bookService).updateBook(any(UpdateBookDto.class));
    }

    @Test
    void updateBook_whenCategoryNotFound_shouldReturn404() throws Exception {
        UpdateBookDto updateBookDto = new UpdateBookDto();
        doThrow(new CategoryNotFoundException("Category not found"))
                .when(bookService).updateBook(any(UpdateBookDto.class));

        mockMvc.perform(patch("/api/books/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBookDto)))
                .andExpect(status().isNotFound());

        verify(bookService).updateBook(any(UpdateBookDto.class));
    }

    @Test
    void deleteBook_shouldReturnNoContent() throws Exception {
        doNothing().when(bookService).deleteBook(1L);

        mockMvc.perform(delete("/api/books/admin/1"))
                .andExpect(status().isNoContent());

        verify(bookService).deleteBook(1L);
    }

    @Test
    void deleteBook_whenBookNotFound_shouldReturn404() throws Exception {
        doThrow(new BookNotFoundException("Book not found"))
                .when(bookService).deleteBook(1L);

        mockMvc.perform(delete("/api/books/admin/1"))
                .andExpect(status().isNotFound());

        verify(bookService).deleteBook(1L);
    }
}